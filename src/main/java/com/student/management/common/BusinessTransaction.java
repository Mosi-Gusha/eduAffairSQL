package com.student.management.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessTransaction {
    String businessType() default "";

    String operation() default "";

    String tableName() default "";

    int recordIdArgIndex() default -1;
}
