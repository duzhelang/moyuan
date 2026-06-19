package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.R;
import com.moyuan.entity.AiGeneratedContent;
import com.moyuan.service.AiGeneratedContentService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI内容填充接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiContentController {

    private final AiGeneratedContentService aiGeneratedContentService;

    @Operation(summary = "AI生成预览（不写入数据库）")
    @PostMapping("/preview")
    public R<Map<String, String>> preview(@RequestBody Map<String, Object> request) {
        SecurityUtil.getCurrentUserId();
        String targetType = (String) request.get("targetType");
        Long targetId = Long.valueOf(request.get("targetId").toString());
        String fieldName = (String) request.get("fieldName");

        if (targetType == null || targetId == null || fieldName == null) {
            return R.error("参数不完整：targetType、targetId、fieldName 不能为空");
        }

        String content = aiGeneratedContentService.generatePreview(targetType, targetId, fieldName);
        return R.success(Map.of("content", content));
    }

    @Operation(summary = "提交AI内容到审核表")
    @PostMapping("/submit-review")
    public R<AiGeneratedContent> submitForReview(@RequestBody Map<String, Object> request) {
        SecurityUtil.getCurrentUserId();
        String targetType = (String) request.get("targetType");
        Long targetId = Long.valueOf(request.get("targetId").toString());
        String fieldName = (String) request.get("fieldName");
        String content = (String) request.get("content");

        if (targetType == null || targetId == null || fieldName == null || content == null) {
            return R.error("参数不完整");
        }

        AiGeneratedContent record = aiGeneratedContentService.submitForReview(targetType, targetId, fieldName, content);
        return R.success(record);
    }

    @Operation(summary = "触发AI填充（直接生成并存库）")
    @PostMapping("/fill-content")
    public R<AiGeneratedContent> fillContent(@RequestBody Map<String, Object> request) {
        SecurityUtil.getCurrentUserId();
        String targetType = (String) request.get("targetType");
        Long targetId = Long.valueOf(request.get("targetId").toString());
        String fieldName = (String) request.get("fieldName");

        if (targetType == null || targetId == null || fieldName == null) {
            return R.error("参数不完整：targetType、targetId、fieldName 不能为空");
        }

        AiGeneratedContent record = aiGeneratedContentService.generateContent(targetType, targetId, fieldName);
        return R.success(record);
    }

    @Operation(summary = "查询某目标的AI填充状态")
    @GetMapping("/fill-status/{targetType}/{targetId}")
    public R<List<AiGeneratedContent>> getFillStatus(
            @PathVariable String targetType,
            @PathVariable Long targetId) {
        LambdaQueryWrapper<AiGeneratedContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiGeneratedContent::getTargetType, targetType)
               .eq(AiGeneratedContent::getTargetId, targetId)
               .orderByDesc(AiGeneratedContent::getCreateTime);
        List<AiGeneratedContent> list = aiGeneratedContentService.list(wrapper);
        return R.success(list);
    }
}
