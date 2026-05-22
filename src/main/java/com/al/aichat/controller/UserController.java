package com.al.aichat.controller;

import com.al.aichat.common.Result;
import com.al.aichat.dto.LoginRequest;
import com.al.aichat.dto.RegisterRequest;
import com.al.aichat.dto.UpdatePasswordRequest;
import com.al.aichat.dto.UpdateProfileRequest;
import com.al.aichat.entity.User;
import com.al.aichat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户控制器
 *
 * 只负责接收请求、参数校验和返回结果。
 * 业务逻辑在 UserService，异常统一由 GlobalExceptionHandler 处理。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/user/register
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return Result.success("注册成功", user);
    }

    /**
     * POST /api/user/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = userService.login(request);
        return Result.success("登录成功", data);
    }

    /**
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody UpdateProfileRequest request,
                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        User user = userService.updateProfile(userId, request);
        return Result.success("修改成功", user);
    }

    /**
     * PUT /api/user/password
     */
    @PutMapping("/password")
    public Result<String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request,
                                         HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        userService.updatePassword(userId, request);
        return Result.success("密码修改成功", null);
    }

    /**
     * POST /api/user/avatar  上传头像
     * 前端：multipart/form-data，字段名 file
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success("头像上传成功", avatarUrl);
    }

}
