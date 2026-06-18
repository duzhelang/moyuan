package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.PoetDraft;
import com.moyuan.entity.User;
import com.moyuan.service.PoetDraftService;
import com.moyuan.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PoetDraftController {

    private final PoetDraftService poetDraftService;

    @PostMapping("/poet-drafts")
    public R<Void> saveDraft(@RequestBody PoetDraft draft, HttpServletRequest request) {
        User currentUser = SecurityUtil.getCurrentUser();
        draft.setEditorId(currentUser.getId());
        draft.setIp(getClientIp(request));
        poetDraftService.saveDraft(draft);
        return R.success();
    }

    @GetMapping("/admin/poet-drafts")
    public R<IPage<PoetDraft>> getDraftList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long poetId,
            @RequestParam(required = false) Integer status) {
        return R.success(poetDraftService.getDraftList(pageNum, pageSize, poetId, status));
    }

    @GetMapping("/admin/poet-drafts/{id}")
    public R<PoetDraft> getDraftById(@PathVariable Long id) {
        return R.success(poetDraftService.getDraftById(id));
    }

    @PutMapping("/admin/poet-drafts/{id}/review")
    public R<Void> reviewDraft(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String reviewComment) {
        User currentUser = SecurityUtil.getCurrentUser();
        poetDraftService.reviewDraft(id, status, reviewComment, currentUser.getId());
        return R.success();
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