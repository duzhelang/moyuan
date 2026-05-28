package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.PasswordUpdateRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.dto.response.TokenResponse;
import com.moyuan.entity.User;
import com.moyuan.entity.UserFavorite;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.UserFavoriteMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.UserService;
import com.moyuan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (request.getEmail() != null && userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new TokenResponse(token, 86400000L);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new TokenResponse(token, 86400000L);
    }

    @Override
    public User getUserInfo(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public User updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getBio() != null) user.setBio(request.getBio());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void toggleFavorite(Long userId, Long poemId) {
        UserFavorite existing = userFavoriteMapper.selectOne(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetId, poemId)
                        .eq(UserFavorite::getTargetType, TargetType.POEM.getCode()));
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
        } else {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setTargetId(poemId);
            favorite.setTargetType(TargetType.POEM.getCode());
            userFavoriteMapper.insert(favorite);
        }
    }

    @Override
    public boolean isFavorite(Long userId, Long poemId) {
        return userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetId, poemId)
                        .eq(UserFavorite::getTargetType, TargetType.POEM.getCode())) > 0;
    }
}
