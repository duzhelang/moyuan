package com.moyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.dto.response.TokenResponse;
import com.moyuan.entity.User;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.impl.UserServiceImpl;
import com.moyuan.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void login_成功返回token() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setRole("user");
        user.setStatus(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("test-token");

        TokenResponse tokenResponse = userService.login(request);

        assertNotNull(tokenResponse);
        assertEquals("test-token", tokenResponse.getToken());
    }

    @Test
    void login_用户名不存在抛出异常() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            userService.login(request);
        });
    }

    @Test
    void login_密码错误抛出异常() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setStatus(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> {
            userService.login(request);
        });
    }

    @Test
    void register_成功注册() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> {
            userService.register(request);
        });
    }

    @Test
    void register_用户名已存在抛出异常() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new User());

        assertThrows(BusinessException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    void getUserById_存在时返回用户() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userMapper.selectById(1L)).thenReturn(user);

        User result = userService.getUserInfo(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserInfo_不存在时抛出异常() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            userService.getUserInfo(999L);
        });
    }

    @Test
    void updateUser_成功更新() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@example.com");
        request.setNickname("新昵称");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> {
            userService.updateUserInfo(1L, request);
        });

        assertEquals("new@example.com", user.getEmail());
        assertEquals("新昵称", user.getNickname());
    }

    @Test
    void updateUser_用户不存在抛出异常() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@example.com");

        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            userService.updateUserInfo(999L, request);
        });
    }
}
