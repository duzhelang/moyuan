package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.AiChatSessionService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 「诗宝阁」服务端会话接口。
 * <p>
 * 只负责按「用户 + 角色」存取最近消息；模型调用仍走 {@code /api/ai/chat} 与
 * {@code /api/ai/chat/stream}，两者解耦，互不影响。
 */
@Tag(name = "AI会话接口")
@RestController
@RequestMapping("/api/ai/chat/sessions")
@RequiredArgsConstructor
public class AiChatSessionController {

    private static final Pattern ROLE_ID_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private final AiChatSessionService sessionService;

    @Operation(summary = "读取当前角色的会话")
    @GetMapping("/{roleId}")
    public R<List<Map<String, Object>>> getSession(@PathVariable String roleId) {
        return R.success(sessionService.load(SecurityUtil.getCurrentUserId(), validateRoleId(roleId)));
    }

    @Operation(summary = "保存当前角色的会话")
    @PutMapping("/{roleId}")
    public R<Void> saveSession(@PathVariable String roleId, @RequestBody Map<String, Object> body) {
        Object messages = body == null ? null : body.get("messages");
        if (!(messages instanceof List<?> list)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "messages 不能为空");
        }
        sessionService.save(SecurityUtil.getCurrentUserId(), validateRoleId(roleId), list);
        return R.success();
    }

    @Operation(summary = "清空当前角色的会话")
    @DeleteMapping("/{roleId}")
    public R<Void> clearSession(@PathVariable String roleId) {
        sessionService.clear(SecurityUtil.getCurrentUserId(), validateRoleId(roleId));
        return R.success();
    }

    private String validateRoleId(String roleId) {
        if (roleId == null || !ROLE_ID_PATTERN.matcher(roleId).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "roleId 不合法");
        }
        return roleId;
    }
}
