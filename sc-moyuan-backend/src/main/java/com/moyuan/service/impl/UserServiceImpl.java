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
import com.moyuan.entity.UserHistory;
import com.moyuan.entity.UserLike;
import com.moyuan.enums.TargetType;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.UserFavoriteMapper;
import com.moyuan.mapper.UserHistoryMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.ForumPostService;
import com.moyuan.service.UserService;
import com.moyuan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserHistoryMapper userHistoryMapper;
    private final UserLikeMapper userLikeMapper;
    private final ForumPostService forumPostService;
    @Lazy
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
        
        // 处理兴趣选项
        if (request.getInterests() != null && !request.getInterests().isEmpty()) {
            user.setInterests(String.join(",", request.getInterests()));
        }
        
        user.setStatus(1);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new TokenResponse(token, jwtUtil.getExpiration());
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
        user.setLastLoginIp(getClientIp());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new TokenResponse(token, jwtUtil.getExpiration());
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
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthday() != null) user.setBirthday(request.getBirthday());
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

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        long favoriteCount = userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId));
        
        long postCount = forumPostService.count(
                new LambdaQueryWrapper<com.moyuan.entity.ForumPost>()
                        .eq(com.moyuan.entity.ForumPost::getUserId, userId)
                        .eq(com.moyuan.entity.ForumPost::getDeleted, 0));
        
        long historyCount = userHistoryMapper.selectCount(
                new LambdaQueryWrapper<UserHistory>()
                        .eq(UserHistory::getUserId, userId));
        
        long likeCount = userLikeMapper.selectCount(
                new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId));
        
        stats.put("favoriteCount", favoriteCount);
        stats.put("postCount", postCount);
        stats.put("historyCount", historyCount);
        stats.put("likeCount", likeCount);
        
        return stats;
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
