package com.moyuan.util;

import com.moyuan.entity.User;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUser();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 获取当前登录用户；未登录（如内部调用、单元测试关闭过滤器）时返回 null，不抛异常。
     * 仅用于可选的增强逻辑（如 AI 限流），业务鉴权仍应使用 {@link #getCurrentUser()}。
     */
    public static User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser.getUser() : null;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static Long getCurrentUserIdOrNull() {
        User user = getCurrentUserOrNull();
        return user == null ? null : user.getId();
    }

    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }
}
