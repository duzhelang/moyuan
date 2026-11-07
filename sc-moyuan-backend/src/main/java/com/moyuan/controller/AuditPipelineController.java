package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.AuditRule;
import com.moyuan.entity.User;
import com.moyuan.service.AuditPipelineService;
import com.moyuan.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审核流水线。
 *
 * 权责划分：
 * - 管理员（ADMIN）：建单、分派、改派、规则配置、审核员团队管理，可协审；
 * - 审核员（REVIEWER）：只处理自己的工单，可认领、转交、退回。
 * 具体端点权限见 SecurityConfig。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/audit-pipeline")
@RequiredArgsConstructor
public class AuditPipelineController {

    private final AuditPipelineService auditPipelineService;

    @GetMapping("/board")
    public R<Map<String, Object>> board() {
        User current = SecurityUtil.getCurrentUser();
        return R.success(auditPipelineService.getBoard(
                current == null ? null : current.getId(), isAdmin(current)));
    }

    /** 审核记录：审核员只看自己，管理员可看全组并按审核员筛选 */
    @GetMapping("/records")
    public R<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) Long reviewerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        User current = SecurityUtil.getCurrentUser();
        return R.success(auditPipelineService.getRecords(
                current == null ? null : current.getId(), isAdmin(current),
                page, size, bizType, outcome, reviewerId, startDate, endDate, keyword));
    }

    /** 扫描六类待审数据补建工单；已有工单的不会重复建（管理员） */
    @PostMapping("/scan")
    public R<Map<String, Object>> scan() {
        return R.success(auditPipelineService.scan());
    }

    /** 把所有待分派工单按规则重新分派一次（管理员） */
    @PostMapping("/assign-pending")
    public R<Integer> assignPending() {
        return R.success(auditPipelineService.assignPending());
    }

    @PostMapping("/{id}/claim")
    public R<Void> claim(@PathVariable Long id) {
        User current = SecurityUtil.getCurrentUser();
        auditPipelineService.claim(id, current == null ? null : current.getId(), isAdmin(current));
        return R.success();
    }

    @PostMapping("/{id}/transfer")
    public R<Void> transfer(@PathVariable Long id, @RequestParam Long reviewerId) {
        User current = SecurityUtil.getCurrentUser();
        auditPipelineService.transfer(id, reviewerId,
                current == null ? null : current.getId(), isAdmin(current));
        return R.success();
    }

    @PostMapping("/{id}/return")
    public R<Void> returnToPool(@PathVariable Long id) {
        User current = SecurityUtil.getCurrentUser();
        auditPipelineService.returnToPool(id, current == null ? null : current.getId(), isAdmin(current));
        return R.success();
    }

    @GetMapping("/rules")
    public R<List<Map<String, Object>>> rules() {
        return R.success(auditPipelineService.listRules());
    }

    @PostMapping("/rules")
    public R<Void> saveRule(@RequestBody AuditRule rule) {
        auditPipelineService.saveRule(rule);
        return R.success();
    }

    @DeleteMapping("/rules/{id}")
    public R<Void> deleteRule(@PathVariable Long id) {
        auditPipelineService.deleteRule(id);
        return R.success();
    }

    @GetMapping("/reviewers")
    public R<List<Map<String, Object>>> reviewers() {
        return R.success(auditPipelineService.listReviewers());
    }

    /** 可登记为审核员的候选账号（role=reviewer 且尚未登记） */
    @GetMapping("/reviewer-candidates")
    public R<List<Map<String, Object>>> reviewerCandidates() {
        return R.success(auditPipelineService.listReviewerCandidates());
    }

    /** 新增审核员 */
    @PostMapping("/reviewers")
    public R<Void> addReviewer(@RequestBody Map<String, Object> params) {
        auditPipelineService.saveReviewer(
                null,
                toLong(params.get("userId")),
                (String) params.get("displayName"),
                (String) params.get("specialties"),
                toInt(params.get("maxConcurrent")),
                toInt(params.get("status")));
        return R.success();
    }

    /** 修改审核员：工位名、擅长类型、并发上限、启停 */
    @PutMapping("/reviewers/{id}")
    public R<Void> updateReviewer(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        auditPipelineService.saveReviewer(
                id,
                null,
                (String) params.get("displayName"),
                (String) params.get("specialties"),
                toInt(params.get("maxConcurrent")),
                toInt(params.get("status")));
        return R.success();
    }

    /** 删除审核员：有在手工单时不允许删除，应先改派或停用 */
    @DeleteMapping("/reviewers/{id}")
    public R<Void> deleteReviewer(@PathVariable Long id) {
        auditPipelineService.deleteReviewer(id);
        return R.success();
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    private Long toLong(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private Integer toInt(Object value) {
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
