package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.PasswordUpdateRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.dto.response.TokenResponse;
import com.moyuan.entity.User;

public interface UserService extends IService<User> {
    TokenResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    User getUserInfo(Long id);
    User updateUserInfo(Long userId, UserUpdateRequest request);
    void updatePassword(Long userId, PasswordUpdateRequest request);
    void toggleFavorite(Long userId, Long poemId);
    boolean isFavorite(Long userId, Long poemId);
}
