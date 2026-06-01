package com.smartexpense.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，由 {@code OperLogAspect} 切面自动记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 操作标题，如「创建报销单」「审批报销单」 */
    String value() default "";
}
