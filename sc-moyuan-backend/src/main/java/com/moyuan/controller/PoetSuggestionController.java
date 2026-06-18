package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.PoetSuggestion;
import com.moyuan.entity.User;
import com.moyuan.service.PoetSuggestionService;
import com.moyuan.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PoetSuggestionController {

    private final PoetSuggestionService poetSuggestionService;

    @PostMapping("/poet-suggestions")
    public R<Void> submitSuggestion(@RequestBody PoetSuggestion suggestion, HttpServletRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        suggestion.setUserId(currentUser.getId());
        suggestion.setIp(getClientIp(request));
        suggestion.setContent(sanitizeInput(suggestion.getContent()));
        poetSuggestionService.submitSuggestion(suggestion);
        return R.success();
    }

    @GetMapping("/admin/poet-suggestions")
    public R<IPage<PoetSuggestion>> getSuggestionList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return R.success(poetSuggestionService.getSuggestionList(pageNum, pageSize, status));
    }

    @PutMapping("/admin/poet-suggestions/{id}/review")
    public R<Void> reviewSuggestion(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String reviewComment) {
        User currentUser = SecurityUtil.getCurrentUser();
        poetSuggestionService.reviewSuggestion(id, status, reviewComment, currentUser.getId());
        return R.success();
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "")
                .replaceAll("javascript:", "")
                .replaceAll("on\\w+\\s*=", "")
                .trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
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
