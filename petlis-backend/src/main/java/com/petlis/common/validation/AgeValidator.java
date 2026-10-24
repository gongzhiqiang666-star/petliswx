package com.petlis.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 年龄格式验证器
 * 验证年龄格式是否符合"Y/M"规范：
 * - "3Y7M": 3岁7个月
 * - "5M": 5个月
 * - "3Y": 3岁
 * 总月数不超过1200（100岁）
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public class AgeValidator implements ConstraintValidator<AgeFormat, String> {

    private static final Pattern AGE_PATTERN =
        Pattern.compile("^(\\d+)Y(\\d+)M$|^(\\d+)Y$|^(\\d+)M$");

    @Override
    public boolean isValid(String age, ConstraintValidatorContext context) {
        // 空值检查
        if (age == null || age.trim().isEmpty()) {
            return false;
        }

        // 格式匹配
        Matcher matcher = AGE_PATTERN.matcher(age);
        if (!matcher.matches()) {
            return false;
        }

        // 提取年份和月份
        int years = 0, months = 0;
        if (matcher.group(1) != null && matcher.group(2) != null) {
            // "3Y7M" 格式
            years = Integer.parseInt(matcher.group(1));
            months = Integer.parseInt(matcher.group(2));
        } else if (matcher.group(3) != null) {
            // "3Y" 格式
            years = Integer.parseInt(matcher.group(3));
        } else if (matcher.group(4) != null) {
            // "5M" 格式
            months = Integer.parseInt(matcher.group(4));
        }

        // 验证范围：总月数不超过1200（100岁）且大于0
        int totalMonths = years * 12 + months;
        return totalMonths > 0 && totalMonths <= 1200;
    }
}
