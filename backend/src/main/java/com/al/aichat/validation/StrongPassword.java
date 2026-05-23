package com.al.aichat.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 自定义密码强度校验注解
 * <p>
 * 使用方式：在 DTO 字段上添加 @StrongPassword
 * <p>
 * 校验规则：
 * - 至少 8 位
 * - 至少包含 1 个大写字母
 * - 至少包含 1 个小写字母
 * - 至少包含 1 个数字
 * - 可选：至少包含 1 个特殊字符
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "密码必须至少 8 位，且包含大写字母、小写字母和数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}