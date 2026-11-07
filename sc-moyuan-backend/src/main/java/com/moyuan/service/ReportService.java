package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.dto.request.ReportCreateRequest;
import com.moyuan.dto.request.ReportHandleRequest;
import com.moyuan.entity.UserReport;

import java.util.Map;

/**
 * 举报（用户对用户/内容违规）。系统 Bug 报错请走 {@code RepairOrderService}，两者互不混用。
 */
public interface ReportService {

    UserReport submit(Long reporterId, ReportCreateRequest request);

    IPage<UserReport> getMyReports(Long reporterId, int pageNum, int pageSize, String status);

    boolean hasReported(Long reporterId, String targetType, Long targetId);

    /** AI 风控命中后自动建单（reporterId=0），已存在则跳过 */
    void createAiReport(String targetType, Long targetId, Long targetUserId, String riskLevel, String reason);

    IPage<UserReport> adminPage(int pageNum, int pageSize, String status, String targetType,
                                Integer aiFlag, String keyword);

    Map<String, Object> stats();

    void handle(Long handlerId, Long reportId, ReportHandleRequest request);
}
