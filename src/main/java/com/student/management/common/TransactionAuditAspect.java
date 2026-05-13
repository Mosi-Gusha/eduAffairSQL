package com.student.management.common;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.student.management.security.CurrentUserContext;
import com.student.management.security.SessionUser;
import com.student.management.service.TransactionAuditService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Aspect
@Component
public class TransactionAuditAspect {
    private final TransactionAuditService auditService;
    private final PlatformTransactionManager transactionManager;

    public TransactionAuditAspect(TransactionAuditService auditService, PlatformTransactionManager transactionManager) {
        this.auditService = auditService;
        this.transactionManager = transactionManager;
    }

    @Around("@annotation(com.student.management.common.BusinessTransaction)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        BusinessTransaction businessTransaction = method.getAnnotation(BusinessTransaction.class);
        if (businessTransaction == null) {
            return joinPoint.proceed();
        }
        Object[] args = joinPoint.getArgs();
        String transactionId = UUID.randomUUID().toString();
        String businessType = valueOrDefault(businessTransaction.businessType(), toSnakeCase(method.getName()));
        String operation = normalizeOperation(valueOrDefault(businessTransaction.operation(), inferOperation(method.getName())));
        String tableName = blankToNull(businessTransaction.tableName());
        Long actorUserId = actorUserId(args);
        Long recordId = recordId(args, businessTransaction.recordIdArgIndex());

        auditService.start(transactionId, businessType, actorUserId,
                "method=" + method.getDeclaringClass().getSimpleName() + "." + method.getName());
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            return template.execute(status -> {
                TransactionAuditContext.begin(transactionId);
                try {
                    Object result = joinPoint.proceed();
                    if (TransactionAuditContext.entryCount() == 0 && tableName != null) {
                        auditService.logStep(operation, tableName, recordId, "success", successMessage(result));
                    }
                    auditService.finishCommitted(transactionId);
                    return result;
                } catch (Throwable ex) {
                    throw new BusinessInvocationException(ex);
                } finally {
                    TransactionAuditContext.clear();
                }
            });
        } catch (BusinessInvocationException ex) {
            Throwable original = ex.original();
            auditRollback(transactionId, tableName, recordId, original);
            throw original;
        } catch (RuntimeException ex) {
            auditRollback(transactionId, tableName, recordId, ex);
            throw ex;
        }
    }

    private void auditRollback(String transactionId, String tableName, Long recordId, Throwable failure) {
        try {
            auditService.rollback(transactionId, tableName, recordId, failureMessage(failure));
        } catch (RuntimeException auditFailure) {
            failure.addSuppressed(auditFailure);
        }
    }

    private Long actorUserId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof SessionUser user) {
                return user.id();
            }
        }
        return CurrentUserContext.get().map(SessionUser::id).orElse(null);
    }

    private Long recordId(Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            return null;
        }
        Object value = args[index];
        if (value instanceof SessionUser user) {
            return user.id();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String successMessage(Object result) {
        if (result instanceof Map<?, ?> map) {
            Object message = map.get("message");
            if (message != null) {
                return String.valueOf(message);
            }
        }
        return "committed";
    }

    private String failureMessage(Throwable ex) {
        Throwable current = ex;
        Throwable root = ex;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String inferOperation(String methodName) {
        if (methodName.startsWith("create")) {
            return "INSERT";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("stop")) {
            return "DELETE";
        }
        if (methodName.startsWith("update") || methodName.startsWith("enable") || methodName.startsWith("disable")
                || methodName.startsWith("start") || methodName.startsWith("change")) {
            return "UPDATE";
        }
        return "UPSERT";
    }

    private String normalizeOperation(String operation) {
        String normalized = operation.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "START", "INSERT", "UPDATE", "DELETE", "UPSERT", "COMMIT", "ROLLBACK" -> normalized;
            default -> "UPSERT";
        };
    }

    private String toSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i += 1) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private static final class BusinessInvocationException extends RuntimeException {
        private final Throwable original;

        private BusinessInvocationException(Throwable original) {
            super(original);
            this.original = original;
        }

        private Throwable original() {
            return original;
        }
    }
}
