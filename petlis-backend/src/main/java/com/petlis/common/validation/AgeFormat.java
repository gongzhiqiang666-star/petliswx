package com.petlis.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 年龄格式验证注解
 * 验证年龄字段是否符合"Y/M"格式，例如："3Y7M"、"5M"、"3Y"
 * 总月数不超过1200（100岁）
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Documented
@Constraint(validatedBy = AgeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AgeFormat {
    String message() default "年龄格式错误，正确格式如：3Y7M、5M、3Y";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
