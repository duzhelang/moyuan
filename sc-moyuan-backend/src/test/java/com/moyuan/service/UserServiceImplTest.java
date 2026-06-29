package com.moyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.request.UserUpdateRequest;
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

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "user")).thenReturn("test-token");

        String token = userService.login(request);

        assertNotNull(token);
        assertEquals("test-token", token);
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

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
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

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

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

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserById_不存在时返回null() {
        when(userMapper.selectById(999L)).thenReturn(null);

        User result = userService.getUserById(999L);

        assertNull(result);
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
            userService.updateUser(1L, request);
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
            userService.updateUser(999L, request);
        });
    }
}
