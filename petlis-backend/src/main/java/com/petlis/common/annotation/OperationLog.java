package com.petlis.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * 用于标记需要记录操作日志的方法
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型 (如: 新增、修改、删除、查询)
     */
    String operation() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
