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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 *
 * 测试注册、登录、修改资料、修改密码等业务逻辑。
 * 使用 Mockito 模拟 UserMapper 和 JwtUtils，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    private UserService userService;

    /**
     * 每个测试方法执行前创建 UserService 实例
     * 手动传入 Mock 对象（由 Mockito 自动创建）
     */
    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, jwtUtils);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册新用户 - 应成功创建用户并加密密码")
    void register_ShouldCreateUserWithEncryptedPassword() {
        // 准备
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setNickname("新用户");

        // 模拟：用户名不存在（selectOne 返回 null）
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        // 模拟：插入成功，并在 insert 时捕获密码（register 返回前会清空密码）
        final String[] capturedPwd = new String[1];
        doAnswer(invocation -> {
            User u = invocation.getArgument(0);
            capturedPwd[0] = u.getPassword();
            return 1;
        }).when(userMapper).insert(any(User.class));

        // 执行
        User result = userService.register(request);

        // 验证
        assertNotNull(result, "注册返回的用户不应为 null");
        assertEquals("newuser", result.getUsername(), "用户名应一致");
        assertEquals("新用户", result.getNickname(), "昵称应一致");
        assertEquals(1, result.getStatus().intValue(), "新用户状态应为正常(1)");

        // 验证密码已加密（在 insert 时捕获）
        assertNotNull(capturedPwd[0], "密码不应为 null");
        assertTrue(capturedPwd[0].startsWith("$2a$"), "密码应使用 BCrypt 加密");
        assertTrue(BCrypt.checkpw("password123", capturedPwd[0]), "BCrypt 加密的密码应能验证通过");
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("注册已存在的用户名 - 应抛出异常")
    void register_WithExistingUsername_ShouldThrowException() {
        // 准备
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        // 模拟：用户名已存在
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new User());

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.register(request),
                "注册已存在的用户名应抛出异常");
        assertEquals("用户名已存在", exception.getMessage());

        // 验证 insert 没有被调用
        verify(userMapper, never()).insert(any());
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录 - 用户名密码正确应返回 token 和用户信息")
    void login_WithValidCredentials_ShouldReturnTokenAndUserInfo() {
        // 准备
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("correctPassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setPassword(BCrypt.hashpw("correctPassword", BCrypt.gensalt()));
        user.setStatus(1);

        // 模拟：查询到用户
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        // 模拟：生成令牌
        when(jwtUtils.generateToken(1L, "testuser")).thenReturn("mock.jwt.token");

        // 执行
        Map<String, Object> result = userService.login(request);

        // 验证
        assertNotNull(result, "登录结果不应为 null");
        assertEquals("mock.jwt.token", result.get("token"), "应返回 token");
        assertEquals(1L, result.get("userId"), "应返回用户ID");
        assertEquals("testuser", result.get("username"), "应返回用户名");
        assertEquals("测试用户", result.get("nickname"), "应返回昵称");
    }

    @Test
    @DisplayName("登录 - 用户名不存在应抛出异常")
    void login_WithNonExistentUsername_ShouldThrowException() {
        // 准备
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        // 模拟：用户不存在
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录 - 密码错误应抛出异常")
    void login_WithWrongPassword_ShouldThrowException() {
        // 准备
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword(BCrypt.hashpw("correctPassword", BCrypt.gensalt()));
        user.setStatus(1);

        // 模拟：查询到用户
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    @DisplayName("登录 - 账号被禁用应抛出异常")
    void login_WithDisabledAccount_ShouldThrowException() {
        // 准备
        LoginRequest request = new LoginRequest();
        request.setUsername("disableduser");
        request.setPassword("password123");

        User user = new User();
        user.setUsername("disableduser");
        user.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
        user.setStatus(0); // 禁用状态

        // 模拟：查询到用户
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.login(request));
        assertEquals("账号已被禁用", exception.getMessage());
    }

    // ==================== 修改资料测试 ====================

    @Test
    @DisplayName("修改昵称 - 应成功更新")
    void updateProfile_ShouldUpdateNickname() {
        // 准备
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setNickname("旧昵称");
        existingUser.setPassword("encodedPassword");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        // avatar 不传（null），表示不修改

        // 模拟
        when(userMapper.selectById(1L)).thenReturn(existingUser);

        // 执行
        User result = userService.updateProfile(1L, request);

        // 验证
        assertEquals("新昵称", result.getNickname(), "昵称应更新为新值");
        assertNull(result.getPassword(), "返回的用户信息不应包含密码");

        // 验证 updateById 被调用，且传入的 user 包含更新后的昵称
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals("新昵称", userCaptor.getValue().getNickname());
    }

    @Test
    @DisplayName("修改资料 - 用户不存在应抛出异常")
    void updateProfile_WithNonExistentUser_ShouldThrowException() {
        // 模拟：用户不存在
        when(userMapper.selectById(999L)).thenReturn(null);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");

        // 执行 & 验证
        assertThrows(RuntimeException.class,
                () -> userService.updateProfile(999L, request),
                "不存在的用户应抛出异常");
    }

    // ==================== 修改密码测试 ====================

    @Test
    @DisplayName("修改密码 - 旧密码正确应成功更新")
    void updatePassword_WithCorrectOldPassword_ShouldSucceed() {
        // 准备
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword(BCrypt.hashpw("oldPassword", BCrypt.gensalt()));

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword123");

        // 模拟
        when(userMapper.selectById(1L)).thenReturn(existingUser);

        // 执行
        userService.updatePassword(1L, request);

        // 验证：密码已更新为新密码的 BCrypt 加密结果
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        String updatedPassword = userCaptor.getValue().getPassword();
        assertNotNull(updatedPassword, "更新后的密码不应为 null");
        assertTrue(updatedPassword.startsWith("$2a$"), "新密码应使用 BCrypt 加密");
        assertTrue(BCrypt.checkpw("newPassword123", updatedPassword), "新密码应能正确验证");
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误应抛出异常")
    void updatePassword_WithWrongOldPassword_ShouldThrowException() {
        // 准备
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword(BCrypt.hashpw("correctOldPassword", BCrypt.gensalt()));

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("wrongOldPassword");
        request.setNewPassword("newPassword123");

        // 模拟
        when(userMapper.selectById(1L)).thenReturn(existingUser);

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(1L, request));
        assertEquals("原密码错误", exception.getMessage());

        // 验证 updateById 没有被调用
        verify(userMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("修改密码 - 新密码少于6位应抛出异常")
    void updatePassword_WithTooShortNewPassword_ShouldThrowException() {
        // 准备
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword(BCrypt.hashpw("oldPassword", BCrypt.gensalt()));

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("12345"); // 只有5位，不满足6位要求

        // 模拟
        when(userMapper.selectById(1L)).thenReturn(existingUser);

        // 执行 & 验证
        RuntimeException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(1L, request));
        assertEquals("新密码至少需要6位", exception.getMessage());
    }
}
