package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.dto.request.ReportCreateRequest;
import com.moyuan.dto.request.ReportHandleRequest;
import com.moyuan.entity.Comment;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.Poem;
import com.moyuan.entity.User;
import com.moyuan.entity.UserReport;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.CommentMapper;
import com.moyuan.mapper.ForumPostMapper;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.mapper.UserReportMapper;
import com.moyuan.service.NotificationService;
import com.moyuan.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<UserReportMapper, UserReport> implements ReportService {

    private static final Set<String> TARGET_TYPES = Set.of("post", "comment", "poem", "user");
    private static final Set<String> REASON_TYPES = Set.of(
            "spam", "abuse", "porn", "plagiarism", "illegal", "other");
    private static final Set<String> HANDLE_STATUSES = Set.of("processing", "accepted", "rejected");

    private final UserReportMapper reportMapper;
    private final ForumPostMapper forumPostMapper;
    private final CommentMapper commentMapper;
    private final PoemMapper poemMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public UserReport submit(Long reporterId, ReportCreateRequest request) {
        String targetType = request.getTargetType().trim().toLowerCase();
        if (!TARGET_TYPES.contains(targetType)) {
            throw new BusinessException("不支持的举报对象类型");
        }
        if (!REASON_TYPES.contains(request.getReasonType())) {
            throw new BusinessException("不支持的举报原因");
        }

        Target target = resolveTarget(targetType, request.getTargetId());
        if (target.userId != null && target.userId.equals(reporterId)) {
            throw new BusinessException("不能举报自己发布的内容");
        }
        boolean reported = reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getReporterId, reporterId)
                .eq(UserReport::getTargetType, targetType)
                .eq(UserReport::getTargetId, request.getTargetId())) > 0;
        if (reported) {
            throw new BusinessException("你已举报过该内容，请勿重复提交");
        }

        UserReport report = new UserReport();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setTargetUserId(target.userId);
        report.setReasonType(request.getReasonType());
        report.setReason(request.getReason());
        report.setEvidence(request.getEvidence());
        report.setStatus("pending");
        report.setAiFlag(0);
        reportMapper.insert(report);
        return report;
    }

    @Override
    public IPage<UserReport> getMyReports(Long reporterId, int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<UserReport> wrapper = new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getReporterId, reporterId)
                .orderByDesc(UserReport::getCreateTime);
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserReport::getStatus, status);
        }
        return reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public boolean hasReported(Long reporterId, String targetType, Long targetId) {
        if (reporterId == null || reporterId <= 0 || !StringUtils.hasText(targetType) || targetId == null) {
            return false;
        }
        return reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getReporterId, reporterId)
                .eq(UserReport::getTargetType, targetType)
                .eq(UserReport::getTargetId, targetId)) > 0;
    }

    @Override
    public void createAiReport(String targetType, Long targetId, Long targetUserId,
                               String riskLevel, String reason) {
        if (targetId == null || !TARGET_TYPES.contains(targetType)) {
            return;
        }
        try {
            boolean exists = reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                    .eq(UserReport::getReporterId, 0L)
                    .eq(UserReport::getTargetType, targetType)
                    .eq(UserReport::getTargetId, targetId)) > 0;
            if (exists) {
                return;
            }
            UserReport report = new UserReport();
            report.setReporterId(0L);
            report.setTargetType(targetType);
            report.setTargetId(targetId);
            report.setTargetUserId(targetUserId);
            report.setReasonType("ai_auto");
            report.setReason(cut("AI风控命中：" + riskLevel, 1000));
            report.setStatus("pending");
            report.setAiFlag(1);
            report.setAiRiskLevel(riskLevel);
            report.setAiReason(cut(reason, 1000));
            reportMapper.insert(report);
            // 高风险自动限流：告知被处理作者已转人工复核
            if ("high".equals(riskLevel)) {
                notificationService.push(targetUserId, "system", "内容触发自动风控",
                        "你发布的" + targetLabel(targetType) + "触发自动风控，已暂时限制展示并转人工复核。",
                        targetType, targetId, targetLink(targetType, targetId));
            }
        } catch (DuplicateKeyException e) {
            log.debug("AI 风控举报单已存在 targetType={} targetId={}", targetType, targetId);
        } catch (Exception e) {
            // 风控建单属于增强能力，失败不能阻断内容发布
            log.warn("AI 风控举报单创建失败 targetType={} targetId={}", targetType, targetId, e);
        }
    }

    @Override
    public IPage<UserReport> adminPage(int pageNum, int pageSize, String status, String targetType,
                                       Integer aiFlag, String keyword) {
        LambdaQueryWrapper<UserReport> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserReport::getStatus, status);
        }
        if (StringUtils.hasText(targetType)) {
            wrapper.eq(UserReport::getTargetType, targetType);
        }
        if (aiFlag != null) {
            wrapper.eq(UserReport::getAiFlag, aiFlag);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(UserReport::getReason, keyword)
                    .or().like(UserReport::getAiReason, keyword)
                    .or().like(UserReport::getHandleNote, keyword));
        }
        wrapper.orderByDesc(UserReport::getCreateTime);
        return reportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", reportMapper.selectCount(null));
        map.put("pending", countByStatus("pending"));
        map.put("processing", countByStatus("processing"));
        map.put("accepted", countByStatus("accepted"));
        map.put("rejected", countByStatus("rejected"));
        map.put("aiPending", reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getAiFlag, 1).eq(UserReport::getStatus, "pending")));
        map.put("today", reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .ge(UserReport::getCreateTime, LocalDate.now().atStartOfDay())));
        return map;
    }

    @Override
    @Transactional
    public void handle(Long handlerId, Long reportId, ReportHandleRequest request) {
        UserReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        String status = request.getStatus();
        if (!HANDLE_STATUSES.contains(status)) {
            throw new BusinessException("不支持的处理状态");
        }

        String action = StringUtils.hasText(request.getAction()) ? request.getAction() : "none";
        boolean contentHandled = false;
        if ("accepted".equals(status) && ("hide".equals(action) || "delete".equals(action))) {
            contentHandled = applyAction(report, action);
        }

        report.setStatus(status);
        report.setHandleNote(cut(request.getHandleNote(), 500));
        report.setHandlerId(handlerId);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        notifyReporter(report);
        if (contentHandled) {
            notifyTarget(report, action);
        }
    }

    /** 按举报对象执行限流动作；返回是否真的处理了内容 */
    private boolean applyAction(UserReport report, String action) {
        boolean delete = "delete".equals(action);
        String note = "举报处理：" + ("delete".equals(action) ? "内容已删除" : "内容已隐藏");
        switch (report.getTargetType()) {
            case "post": {
                ForumPost post = forumPostMapper.selectById(report.getTargetId());
                if (post == null) {
                    return false;
                }
                if (delete) {
                    forumPostMapper.deleteById(post.getId());
                } else {
                    post.setStatus(2);
                    post.setAuditReason(note);
                    post.setAuditTime(LocalDateTime.now());
                    forumPostMapper.updateById(post);
                }
                return true;
            }
            case "comment": {
                Comment comment = commentMapper.selectById(report.getTargetId());
                if (comment == null) {
                    return false;
                }
                if (delete) {
                    commentMapper.deleteById(comment.getId());
                } else {
                    comment.setStatus(0);
                    comment.setAuditReason(note);
                    comment.setAuditTime(LocalDateTime.now());
                    commentMapper.updateById(comment);
                }
                return true;
            }
            default:
                // 诗词/用户类举报只记录处理结论，不在此做删除，避免误伤整首作品或账号
                return false;
        }
    }

    private void notifyReporter(UserReport report) {
        if (report.getReporterId() == null || report.getReporterId() <= 0) {
            return;
        }
        if ("processing".equals(report.getStatus())) {
            return;
        }
        boolean accepted = "accepted".equals(report.getStatus());
        String title = accepted ? "举报已成立" : "举报未成立";
        String content = "你举报的" + targetLabel(report.getTargetType()) + "已处理："
                + (accepted ? "举报成立" : "经核实未发现违规")
                + (StringUtils.hasText(report.getHandleNote()) ? "。处理说明：" + report.getHandleNote() : "。");
        notificationService.push(report.getReporterId(), "report_result", title, content,
                "report", report.getId(), "/notifications");
    }

    private void notifyTarget(UserReport report, String action) {
        if (report.getTargetUserId() == null || report.getTargetUserId() <= 0) {
            return;
        }
        String content = "你发布的" + targetLabel(report.getTargetType())
                + ("delete".equals(action) ? "因违规已被删除" : "因违规已被隐藏")
                + (StringUtils.hasText(report.getHandleNote()) ? "。处理说明：" + report.getHandleNote() : "。");
        notificationService.push(report.getTargetUserId(), "report_result", "内容处理通知", content,
                report.getTargetType(), report.getTargetId(),
                targetLink(report.getTargetType(), report.getTargetId()));
    }

    private Target resolveTarget(String targetType, Long targetId) {
        switch (targetType) {
            case "post": {
                ForumPost post = forumPostMapper.selectById(targetId);
                if (post == null) {
                    throw new BusinessException("举报的帖子不存在或已删除");
                }
                return new Target(post.getUserId());
            }
            case "comment": {
                Comment comment = commentMapper.selectById(targetId);
                if (comment == null) {
                    throw new BusinessException("举报的评论不存在或已删除");
                }
                return new Target(comment.getUserId());
            }
            case "poem": {
                Poem poem = poemMapper.selectById(targetId);
                if (poem == null) {
                    throw new BusinessException("举报的诗词不存在或已删除");
                }
                return new Target(null);
            }
            default: {
                User user = userMapper.selectById(targetId);
                if (user == null) {
                    throw new BusinessException("举报的用户不存在");
                }
                return new Target(user.getId());
            }
        }
    }

    private long countByStatus(String status) {
        return reportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getStatus, status));
    }

    private String targetLabel(String targetType) {
        return switch (targetType == null ? "" : targetType) {
            case "post" -> "帖子";
            case "comment" -> "评论";
            case "poem" -> "诗词";
            case "user" -> "用户";
            default -> "内容";
        };
    }

    private String targetLink(String targetType, Long targetId) {
        if (targetId == null) {
            return null;
        }
        return switch (targetType == null ? "" : targetType) {
            case "post" -> "/forum/" + targetId;
            case "poem" -> "/poem/" + targetId;
            case "comment" -> {
                Comment comment = commentMapper.selectById(targetId);
                if (comment == null || comment.getTargetId() == null) {
                    yield null;
                }
                yield Integer.valueOf(2).equals(comment.getTargetType())
                        ? "/forum/" + comment.getTargetId()
                        : "/poem/" + comment.getTargetId();
            }
            case "user" -> "/user/" + targetId + "/homepage";
            default -> null;
        };
    }

    private String cut(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private record Target(Long userId) {
    }
}
