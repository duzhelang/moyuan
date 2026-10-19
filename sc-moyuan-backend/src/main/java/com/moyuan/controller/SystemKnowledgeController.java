package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.SystemKnowledge;
import com.moyuan.service.SystemKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统知识库公开接口（供 AI 小助手查阅系统用法）
 */
@Slf4j
@Tag(name = "系统知识库")
@RestController
@RequestMapping("/api/system-knowledge")
@RequiredArgsConstructor
public class SystemKnowledgeController {

    private final SystemKnowledgeService systemKnowledgeService;

    @Operation(summary = "获取启用中的系统知识条目")
    @GetMapping
    public R<List<SystemKnowledge>> getEnabledList() {
        try {
            return R.success(systemKnowledgeService.getEnabledList());
        } catch (Exception e) {
            log.error("查询系统知识库列表失败", e);
            return R.error("查询系统知识库列表失败");
        }
    }
}
