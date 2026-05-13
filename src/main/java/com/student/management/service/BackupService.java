package com.student.management.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.student.management.common.ApiException;
import com.student.management.mapper.AdminMapper;
import com.student.management.security.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final AdminMapper adminMapper;

    @Value("${app.backup.enabled:true}")
    private boolean enabled;

    @Value("${app.backup.script-path:}")
    private String scriptPath;

    @Value("${app.backup.windows-script-path:scripts/backup_database.ps1}")
    private String windowsScriptPath;

    @Value("${app.backup.unix-script-path:scripts/backup_database.sh}")
    private String unixScriptPath;

    @Value("${app.backup.directory:backups}")
    private String backupDirectory;

    @Value("${app.backup.retain-count:10}")
    private int retainCount;

    @Value("${app.backup.timeout-seconds:300}")
    private long timeoutSeconds;

    @Value("${app.backup.database-name:test}")
    private String databaseName;

    @Value("${app.backup.host:localhost}")
    private String databaseHost;

    @Value("${app.backup.port:3306}")
    private String databasePort;

    @Value("${spring.datasource.username}")
    private String databaseUser;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    public BackupService(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    public Map<String, Object> backupRecords(Integer page, Integer pageSize) {
        int safePageSize = AdminService.clampPageSize(pageSize);
        long total = adminMapper.countBackupRecords();
        int totalPages = AdminService.totalPages(total, safePageSize);
        int safePage = AdminService.clampPage(page, totalPages);
        int offset = (safePage - 1) * safePageSize;
        return AdminService.mapOf(
                "rows", adminMapper.listBackupRecords(safePageSize, offset),
                "page", safePage,
                "pageSize", safePageSize,
                "total", total,
                "totalPages", totalPages
        );
    }

    public Map<String, Object> runManualBackup(SessionUser user) {
        return runBackup("manual", user.id());
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}", zone = "${app.backup.zone:Asia/Shanghai}")
    public void runScheduledBackup() {
        if (!enabled) {
            return;
        }
        try {
            runBackup("scheduled", null);
        } catch (RuntimeException ex) {
            log.warn("Scheduled database backup failed", ex);
        }
    }

    private Map<String, Object> runBackup(String triggerType, Long actorUserId) {
        if (!enabled) {
            throw new ApiException(400, "数据库备份功能未启用");
        }
        Path script = resolvePath(selectedScriptPath());
        if (!Files.isRegularFile(script)) {
            throw new ApiException(500, "备份脚本不存在：" + script);
        }

        Path outputFile = null;
        try {
            Files.createDirectories(resolvePath(backupDirectory));
            outputFile = Files.createTempFile("teaching-backup-", ".log");
            ProcessBuilder builder = new ProcessBuilder(command(script, triggerType, actorUserId));
            builder.redirectErrorStream(true);
            builder.redirectOutput(outputFile.toFile());
            Map<String, String> environment = builder.environment();
            put(environment, "DB_HOST", databaseHost);
            put(environment, "DB_PORT", databasePort);
            put(environment, "DB_NAME", databaseName);
            put(environment, "DB_USER", databaseUser);
            put(environment, "DB_PASSWORD", databasePassword);

            Process process = builder.start();
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new ApiException(500, "数据库备份超时");
            }
            String output = readProcessOutput(outputFile).trim();
            if (process.exitValue() != 0) {
                throw new ApiException(500, "数据库备份失败：" + lastOutput(output));
            }
            return AdminService.mapOf(
                    "message", "数据库备份已完成",
                    "output", lastOutput(output)
            );
        } catch (IOException ex) {
            throw new ApiException(500, "无法执行数据库备份脚本：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(500, "数据库备份被中断");
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException ignored) {
                    log.debug("Failed to delete backup process output file {}", outputFile);
                }
            }
        }
    }

    private List<String> command(Path script, String triggerType, Long actorUserId) {
        String fileName = script.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".ps1")) {
            return powershellCommand(script, triggerType, actorUserId);
        }
        return shellCommand(script, triggerType, actorUserId);
    }

    private List<String> powershellCommand(Path script, String triggerType, Long actorUserId) {
        List<String> command = new ArrayList<>();
        command.add(isWindows() ? "powershell.exe" : "pwsh");
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(script.toString());
        command.add("-DbName");
        command.add(databaseName);
        command.add("-BackupDir");
        command.add(resolvePath(backupDirectory).toString());
        command.add("-RetainCount");
        command.add(String.valueOf(Math.max(1, retainCount)));
        command.add("-TriggerType");
        command.add(triggerType);
        if (actorUserId != null) {
            command.add("-ActorUserId");
            command.add(String.valueOf(actorUserId));
        }
        return command;
    }

    private List<String> shellCommand(Path script, String triggerType, Long actorUserId) {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add(script.toString());
        command.add("--db-name");
        command.add(databaseName);
        command.add("--backup-dir");
        command.add(resolvePath(backupDirectory).toString());
        command.add("--retain-count");
        command.add(String.valueOf(Math.max(1, retainCount)));
        command.add("--trigger-type");
        command.add(triggerType);
        if (actorUserId != null) {
            command.add("--actor-user-id");
            command.add(String.valueOf(actorUserId));
        }
        return command;
    }

    private String selectedScriptPath() {
        if (scriptPath != null && !scriptPath.isBlank()) {
            return scriptPath;
        }
        return isWindows() ? windowsScriptPath : unixScriptPath;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Path resolvePath(String path) {
        Path resolved = Path.of(path);
        if (resolved.isAbsolute()) {
            return resolved.normalize();
        }
        return Path.of(System.getProperty("user.dir")).resolve(resolved).normalize();
    }

    private void put(Map<String, String> environment, String key, String value) {
        if (value != null) {
            environment.put(key, value);
        }
    }

    private String readProcessOutput(Path outputFile) throws IOException {
        byte[] bytes = Files.readAllBytes(outputFile);
        if (bytes.length == 0) {
            return "";
        }
        List<Charset> charsets = new ArrayList<>();
        addCharset(charsets, bomCharset(bytes));
        addCharset(charsets, StandardCharsets.UTF_8);
        addCharset(charsets, Charset.defaultCharset());
        if (isWindows()) {
            addSupportedCharset(charsets, "GB18030");
            addSupportedCharset(charsets, "GBK");
            addSupportedCharset(charsets, "windows-936");
        }
        for (Charset charset : charsets) {
            try {
                return decodeStrict(bytes, charset);
            } catch (CharacterCodingException ignored) {
                // Try the next likely process-output encoding.
            }
        }
        return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private Charset bomCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFE
                && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        return null;
    }

    private void addSupportedCharset(List<Charset> charsets, String name) {
        try {
            addCharset(charsets, Charset.forName(name));
        } catch (RuntimeException ignored) {
            log.debug("Charset {} is unavailable", name);
        }
    }

    private void addCharset(List<Charset> charsets, Charset charset) {
        if (charset != null && !charsets.contains(charset)) {
            charsets.add(charset);
        }
    }

    private String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private String lastOutput(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        int maxLength = 2000;
        if (output.length() <= maxLength) {
            return output;
        }
        return output.substring(output.length() - maxLength);
    }
}
