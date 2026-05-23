package com.al.aichat.dto;

import com.al.aichat.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @StrongPassword
    private String password;

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
}
