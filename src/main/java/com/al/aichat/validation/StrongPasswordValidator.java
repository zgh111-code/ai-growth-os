package com.al.aichat.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码强度校验器
 * <p>
 * 校验规则：
 * - 至少 8 位
 * - 至少包含 1 个大写字母
 * - 至少包含 1 个小写字母
 * - 至少包含 1 个数字
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        // 长度校验
        if (value.length() < MIN_LENGTH) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }

            // 提前退出：三种都满足
            if (hasUppercase && hasLowercase && hasDigit) {
                return true;
            }
        }

        return hasUppercase && hasLowercase && hasDigit;
    }
}