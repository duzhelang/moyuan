package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.SystemKnowledge;
import com.moyuan.service.SystemKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台系统知识库管理接口
 */
@Slf4j
@Tag(name = "后台系统知识库管理接口")
@RestController
@RequestMapping("/api/admin/system-knowledge")
@RequiredArgsConstructor
public class AdminSystemKnowledgeController {

    private final SystemKnowledgeService systemKnowledgeService;

    @Operation(summary = "分页查询知识条目列表")
    @GetMapping
    public R<Map<String, Object>> listKnowledge(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<SystemKnowledge> page = systemKnowledgeService.getAdminList(pageNum, pageSize, category, keyword);
            Map<String, Object> result = new HashMap<>();
            result.put("records", page.getRecords());
            result.put("total", page.getTotal());
            result.put("pageNum", page.getCurrent());
            result.put("pageSize", page.getSize());
            return R.success(result);
        } catch (Exception e) {
            log.error("查询系统知识库列表失败", e);
            return R.error("查询系统知识库列表失败");
        }
    }

    @Operation(summary = "获取知识条目详情")
    @GetMapping("/{id}")
    public R<SystemKnowledge> getKnowledge(@PathVariable Long id) {
        SystemKnowledge knowledge = systemKnowledgeService.getById(id);
        if (knowledge == null) {
            return R.error("条目不存在");
        }
        return R.success(knowledge);
    }

    @Operation(summary = "新建知识条目")
    @PostMapping
    public R<SystemKnowledge> createKnowledge(@RequestBody SystemKnowledge knowledge) {
        try {
            R<SystemKnowledge> invalid = validateRequired(knowledge);
            if (invalid != null) {
                return invalid;
            }
            long duplicated = systemKnowledgeService.lambdaQuery()
                    .eq(SystemKnowledge::getCode, knowledge.getCode())
                    .count();
            if (duplicated > 0) {
                return R.error("条目标识已存在");
            }
            knowledge.setId(null);
            if (knowledge.getIsShow() == null) knowledge.setIsShow(1);
            if (knowledge.getSortOrder() == null) knowledge.setSortOrder(0);
            return R.success(systemKnowledgeService.createKnowledge(knowledge));
        } catch (Exception e) {
            log.error("新建系统知识条目失败", e);
            return R.error("新建失败");
        }
    }

    @Operation(summary = "更新知识条目")
    @PutMapping("/{id}")
    public R<SystemKnowledge> updateKnowledge(@PathVariable Long id, @RequestBody SystemKnowledge knowledge) {
        try {
            R<SystemKnowledge> invalid = validateRequired(knowledge);
            if (invalid != null) {
                return invalid;
            }
            SystemKnowledge existing = systemKnowledgeService.getById(id);
            if (existing == null) {
                return R.error("条目不存在");
            }
            long duplicated = systemKnowledgeService.lambdaQuery()
                    .eq(SystemKnowledge::getCode, knowledge.getCode())
                    .ne(SystemKnowledge::getId, id)
                    .count();
            if (duplicated > 0) {
                return R.error("条目标识已存在");
            }
            return R.success(systemKnowledgeService.updateKnowledge(id, knowledge));
        } catch (Exception e) {
            log.error("更新系统知识条目失败：id={}", id, e);
            return R.error("更新失败");
        }
    }

    @Operation(summary = "启用/停用知识条目")
    @PutMapping("/{id}/show")
    public R<Void> updateShow(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer isShow = params.get("isShow");
        if (isShow == null) {
            return R.error("启用状态不能为空");
        }
        SystemKnowledge knowledge = systemKnowledgeService.getById(id);
        if (knowledge == null) {
            return R.error("条目不存在");
        }
        systemKnowledgeService.updateShow(id, isShow);
        return R.success();
    }

    @Operation(summary = "删除知识条目（逻辑删除）")
    @DeleteMapping("/{id}")
    public R<Void> deleteKnowledge(@PathVariable Long id) {
        try {
            systemKnowledgeService.deleteKnowledge(id);
            return R.success();
        } catch (Exception e) {
            log.error("删除系统知识条目失败：id={}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 校验必填字段，全部合法返回 null，否则返回统一错误响应
     */
    private R<SystemKnowledge> validateRequired(SystemKnowledge knowledge) {
        if (!StringUtils.hasText(knowledge.getCode())) {
            return R.error("条目标识不能为空");
        }
        if (!StringUtils.hasText(knowledge.getTitle())) {
            return R.error("标题不能为空");
        }
        if (!StringUtils.hasText(knowledge.getCategory())) {
            return R.error("分类不能为空");
        }
        if (!StringUtils.hasText(knowledge.getBody())) {
            return R.error("正文说明不能为空");
        }
        return null;
    }
}
