package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.RefreshTokenRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.response.TokenResponse;
import com.moyuan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.success(userService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.success(userService.login(request));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public R<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return R.success(userService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "登出（使 access token 失效）")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        userService.logout(extractBearerToken(request));
        return R.success();
    }

    /**
     * 从 Authorization 头提取 Bearer token
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
