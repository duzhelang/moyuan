package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.LoginRequest;
import com.moyuan.dto.request.RegisterRequest;
import com.moyuan.dto.response.TokenResponse;
import com.moyuan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
