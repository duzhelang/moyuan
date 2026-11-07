package com.moyuan.service;

import com.moyuan.entity.AuditRule;

import java.util.List;
import java.util.Map;

/**
 * 审核流水线：建单、分派、流转。
 *
 * 权责划分：
 * - 管理员是审核员的上级，负责建单、分派、改派、规则与审核员团队管理，可协审但不进入自动派单池；
 * - 审核员只处理分派给自己或自己认领的工单，不能建单、不能改派给他人之外的调度操作。
 *
 * 弱管控设计：工单只做「调度视图」，不阻断原有审核页。
 * 原审核页审完之后调用 {@link #syncResult} 把结果回写到工单，保证看板与实际状态一致。
 */
public interface AuditPipelineService {

    /** 看板：统计 + 工单列 + 审核员负载；管理员与审核员看到的数据范围不同 */
    Map<String, Object> getBoard(Long currentUserId, boolean admin);

    /** 审核记录：审核员只看自己，管理员可看全组并按审核员筛选 */
    Map<String, Object> getRecords(Long currentUserId, boolean admin, int page, int size,
                                   String bizType, String outcome, Long reviewerId,
                                   String startDate, String endDate, String keyword);

    /** 扫描六类待审数据，补建工单（已存在的跳过），并立即尝试分派 */
    Map<String, Object> scan();

    /** 把所有待分派工单按规则重新分派一次 */
    int assignPending();

    /** 认领：assigned → reviewing；管理员认领视为协审 */
    void claim(Long ticketId, Long currentUserId, boolean admin);

    /** 转交：换审核人，回到 assigned 并重算 SLA；审核员只能转交自己的工单 */
    void transfer(Long ticketId, Long targetReviewerId, Long currentUserId, boolean admin);

    /** 退回：清空审核人，回到 pending，等下次分派；审核员只能退回自己的工单 */
    void returnToPool(Long ticketId, Long currentUserId, boolean admin);

    /**
     * 审核结果回写（弱管控的关键）。
     * 工单不存在则忽略——说明这条内容没走流水线，属正常情况。
     */
    void syncResult(String bizType, Long bizId, boolean approved, String reason);

    List<Map<String, Object>> listRules();

    List<Map<String, Object>> listReviewers();

    /** 可登记为审核员的候选用户（role=reviewer 且尚未登记） */
    List<Map<String, Object>> listReviewerCandidates();

    /** 新增或更新审核员 */
    void saveReviewer(Long id, Long userId, String displayName, String specialties,
                      Integer maxConcurrent, Integer status);

    void deleteReviewer(Long id);

    void saveRule(AuditRule rule);

    void deleteRule(Long id);
}
