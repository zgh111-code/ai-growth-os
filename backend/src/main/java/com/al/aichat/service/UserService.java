package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.dto.LoginRequest;
import com.al.aichat.dto.RegisterRequest;
import com.al.aichat.dto.UpdatePasswordRequest;
import com.al.aichat.dto.UpdateProfileRequest;
import com.al.aichat.entity.User;
import com.al.aichat.mapper.UserMapper;
import com.al.aichat.utils.JwtUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public UserService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    public User register(RegisterRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);

        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setNickname(request.getNickname());
        user.setStatus(1);

        userMapper.insert(user);

        user.setPassword(null);
        return user;
    }

    public Map<String, Object> login(LoginRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());

        return result;
    }

    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatar() != null && !request.getAvatar().trim().isEmpty()) {
            user.setAvatar(request.getAvatar().trim());
        }

        userMapper.updateById(user);

        user.setPassword(null);
        return user;
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!BCrypt.checkpw(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BusinessException("新密码至少需要6位");
        }

        user.setPassword(BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
        userMapper.updateById(user);
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片文件");
        }

        String originalName = file.getOriginalFilename();
        String ext = ".png";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
            if (!ext.matches("\\.(png|jpg|jpeg|gif|webp|svg)$")) {
                throw new BusinessException("不支持的图片格式");
            }
        }

        String filename = userId + "_" + System.currentTimeMillis() + ext;
        filename = filename.replaceAll("[\\\\/:*?\"<>|]", "_");

        try {
            Path uploadDir = Path.of("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path dest = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new BusinessException("头像保存失败");
        }

        String avatarUrl = "/uploads/" + filename;
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        return avatarUrl;
    }
}
