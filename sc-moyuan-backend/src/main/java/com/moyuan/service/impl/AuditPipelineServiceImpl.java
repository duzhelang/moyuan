package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyuan.entity.*;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.*;
import com.moyuan.service.AuditPipelineService;
import com.moyuan.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPipelineServiceImpl implements AuditPipelineService {

    private final AuditTicketMapper ticketMapper;
    private final AuditReviewerMapper reviewerMapper;
    private final AuditRuleMapper ruleMapper;

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final AiGeneratedContentMapper aiGeneratedContentMapper;
    private final PoetProfileMapper poetProfileMapper;
    private final PoetDraftMapper poetDraftMapper;
    private final PoetSuggestionMapper poetSuggestionMapper;
    private final NotificationService notificationService;

    /** 在手工单：分派下去但还没裁定的 */
    private static final List<String> ACTIVE_STATUS = List.of("assigned", "reviewing");

    // ==================== 看板 ====================

    @Override
    public Map<String, Object> getBoard(Long currentUserId, boolean admin) {
        Long myReviewerId = reviewerOfUser(currentUserId);

        List<AuditTicket> all = ticketMapper.selectList(
                new LambdaQueryWrapper<AuditTicket>()
                        .in(AuditTicket::getStatus, List.of("pending", "assigned", "reviewing"))
                        .orderByAsc(AuditTicket::getDeadlineAt));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<AuditTicket> doneWrapper = new LambdaQueryWrapper<AuditTicket>()
                .in(AuditTicket::getStatus, List.of("approved", "rejected"))
                .ge(AuditTicket::getFinishedAt, todayStart)
                .orderByDesc(AuditTicket::getFinishedAt);
        // 审核员只看自己的完成记录，管理员看全组
        if (!admin) {
            doneWrapper.eq(AuditTicket::getReviewerId, myReviewerId);
        }
        List<AuditTicket> doneToday = ticketMapper.selectList(doneWrapper);

        Map<Long, String> nameMap = reviewerNameMap();

        List<Map<String, Object>> pending = new ArrayList<>();
        List<Map<String, Object>> mine = new ArrayList<>();
        List<Map<String, Object>> others = new ArrayList<>();
        for (AuditTicket t : all) {
            Map<String, Object> card = toCard(t, nameMap.get(t.getReviewerId()));
            if ("pending".equals(t.getStatus())) {
                pending.add(card);
            } else if (myReviewerId != null && myReviewerId.equals(t.getReviewerId())) {
                mine.add(card);
            } else if (admin) {
                // 审核员看不到他人正在处理的工单，管理员需要全局视图做调度
                others.add(card);
            }
        }

        List<Map<String, Object>> done = doneToday.stream()
                .limit(30)
                .map(t -> toCard(t, nameMap.get(t.getReviewerId())))
                .collect(Collectors.toList());

        long overdue = admin
                ? all.stream().filter(this::isOverdue).count()
                : mine.stream().filter(m -> Boolean.TRUE.equals(m.get("overdue"))).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", pending.size());
        stats.put("active", admin ? mine.size() + others.size() : mine.size());
        stats.put("doneToday", doneToday.size());
        stats.put("overdue", overdue);
        stats.put("avgMinutes", avgMinutes(doneToday));
        stats.put("total", admin ? ticketMapper.selectCount(null) : mine.size() + doneToday.size());

        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("pending", page(pending));
        columns.put("mine", page(mine));
        columns.put("others", page(others));
        columns.put("done", done);

        Map<String, Object> board = new LinkedHashMap<>();
        board.put("role", admin ? "admin" : "reviewer");
        board.put("stats", stats);
        board.put("columns", columns);
        board.put("reviewers", listReviewers());
        board.put("currentReviewerId", myReviewerId);
        return board;
    }

    /** 单列最多返回 100 条：够看，也不会把整块积压一次性塞给浏览器 */
    private List<Map<String, Object>> page(List<Map<String, Object>> list) {
        return list.size() > 100 ? new ArrayList<>(list.subList(0, 100)) : list;
    }

    @Override
    public Map<String, Object> getRecords(Long currentUserId, boolean admin, int page, int size,
                                          String bizType, String outcome, Long reviewerId,
                                          String startDate, String endDate, String keyword) {
        // 审核员强制只查自己；管理员可看全组，也可按审核员筛选
        Long scopeReviewerId = admin ? reviewerId : reviewerOfUser(currentUserId);
        if (!admin && scopeReviewerId == null) {
            return emptyRecords();
        }

        Page<AuditTicket> pager = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<AuditTicket> query = recordsQuery(scopeReviewerId, bizType, outcome,
                startDate, endDate, keyword, null);
        Page<AuditTicket> result = ticketMapper.selectPage(pager, query);

        long filteredTotal = ticketMapper.selectCount(recordsQuery(scopeReviewerId, bizType, outcome,
                startDate, endDate, keyword, null));
        long total = ticketMapper.selectCount(recordsQuery(scopeReviewerId, bizType, null,
                startDate, endDate, keyword, null));
        long approved = ticketMapper.selectCount(recordsQuery(scopeReviewerId, bizType, null,
                startDate, endDate, keyword, "approved"));
        long rejected = ticketMapper.selectCount(recordsQuery(scopeReviewerId, bizType, null,
                startDate, endDate, keyword, "rejected"));
        List<AuditTicket> sample = ticketMapper.selectList(recordsQuery(scopeReviewerId, bizType, outcome,
                startDate, endDate, keyword, null).last("limit 500"));

        Map<Long, String> nameMap = reviewerNameMap();
        List<Map<String, Object>> records = result.getRecords().stream().map(t -> {
            Map<String, Object> m = toCard(t, nameMap.get(t.getReviewerId()));
            LocalDateTime from = t.getClaimedAt() != null ? t.getClaimedAt() : t.getAssignedAt();
            m.put("durationMinutes", from != null && t.getFinishedAt() != null
                    ? Duration.between(from, t.getFinishedAt()).toMinutes() : 0);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("passRate", total == 0 ? 0 : Math.round(approved * 100.0 / total));
        stats.put("avgMinutes", avgMinutes(sample));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", filteredTotal);
        data.put("page", pager.getCurrent());
        data.put("size", pager.getSize());
        data.put("stats", stats);
        return data;
    }

    private Map<String, Object> emptyRecords() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", 0);
        stats.put("approved", 0);
        stats.put("rejected", 0);
        stats.put("passRate", 0);
        stats.put("avgMinutes", 0);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", List.of());
        data.put("total", 0);
        data.put("page", 1);
        data.put("size", 10);
        data.put("stats", stats);
        return data;
    }

    /** 审核记录统一查询条件；statusOverride 为空时只查已归档的通过/驳回 */
    private LambdaQueryWrapper<AuditTicket> recordsQuery(Long scopeReviewerId, String bizType, String outcome,
                                                         String startDate, String endDate, String keyword,
                                                         String statusOverride) {
        LambdaQueryWrapper<AuditTicket> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(statusOverride)) {
            w.eq(AuditTicket::getStatus, statusOverride);
        } else if (StringUtils.hasText(outcome)) {
            w.eq(AuditTicket::getStatus, outcome);
        } else {
            w.in(AuditTicket::getStatus, List.of("approved", "rejected"));
        }
        if (scopeReviewerId != null) {
            w.eq(AuditTicket::getReviewerId, scopeReviewerId);
        }
        if (StringUtils.hasText(bizType)) {
            w.eq(AuditTicket::getBizType, bizType);
        }
        if (StringUtils.hasText(startDate)) {
            w.ge(AuditTicket::getFinishedAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            w.lt(AuditTicket::getFinishedAt, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(AuditTicket::getTitle, keyword)
                    .or().like(AuditTicket::getAuthorName, keyword));
        }
        w.orderByDesc(AuditTicket::getFinishedAt);
        return w;
    }

    private long avgMinutes(List<AuditTicket> finished) {
        List<Long> spans = new ArrayList<>();
        for (AuditTicket t : finished) {
            LocalDateTime from = t.getClaimedAt() != null ? t.getClaimedAt() : t.getAssignedAt();
            if (from != null && t.getFinishedAt() != null) {
                spans.add(Duration.between(from, t.getFinishedAt()).toMinutes());
            }
        }
        return spans.isEmpty() ? 0 : Math.round(spans.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private Map<String, Object> toCard(AuditTicket t, String reviewerName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("bizType", t.getBizType());
        m.put("bizId", t.getBizId());
        m.put("title", t.getTitle());
        m.put("authorName", t.getAuthorName());
        m.put("dynastyName", t.getDynastyName());
        m.put("contentType", t.getContentType());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("reviewerId", t.getReviewerId());
        m.put("reviewerName", reviewerName);
        m.put("assignNote", t.getAssignNote());
        m.put("result", t.getResult());
        m.put("deadlineAt", t.getDeadlineAt());
        m.put("claimedAt", t.getClaimedAt());
        m.put("finishedAt", t.getFinishedAt());
        m.put("createTime", t.getCreateTime());
        m.put("overdue", isOverdue(t));
        m.put("overdueMinutes", overdueMinutes(t));
        return m;
    }

    private boolean isOverdue(AuditTicket t) {
        return ACTIVE_STATUS.contains(t.getStatus())
                && t.getDeadlineAt() != null
                && t.getDeadlineAt().isBefore(LocalDateTime.now());
    }

    private long overdueMinutes(AuditTicket t) {
        if (!isOverdue(t)) {
            return 0;
        }
        return Duration.between(t.getDeadlineAt(), LocalDateTime.now()).toMinutes();
    }

    private Long reviewerOfUser(Long userId) {
        AuditReviewer r = findReviewerOfUser(userId);
        return r == null ? null : r.getId();
    }

    private AuditReviewer findReviewerOfUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return reviewerMapper.selectOne(
                new LambdaQueryWrapper<AuditReviewer>().eq(AuditReviewer::getUserId, userId).last("limit 1"));
    }

    private Map<Long, String> reviewerNameMap() {
        return reviewerMapper.selectList(null).stream()
                .collect(Collectors.toMap(AuditReviewer::getId, AuditReviewer::getDisplayName, (a, b) -> a));
    }

    // ==================== 扫描建单 ====================

    @Override
    @Transactional
    public Map<String, Object> scan() {
        Set<String> existing = ticketMapper.selectList(null).stream()
                .map(t -> t.getBizType() + "#" + t.getBizId())
                .collect(Collectors.toSet());

        Map<String, Integer> created = new LinkedHashMap<>();
        Map<String, Integer> skipped = new LinkedHashMap<>();
        int[] assignedHolder = {0};
        // 诗人 id → 名称，避免逐条查库
        Map<Long, String> poetNames = poetMapper.selectList(null).stream()
                .filter(x -> x.getId() != null)
                .collect(Collectors.toMap(Poet::getId, x -> x.getName() == null ? "" : x.getName(), (a, b) -> a));

        // 1) 诗词
        List<Poem> poems = poemMapper.selectList(new LambdaQueryWrapper<Poem>()
                .eq(Poem::getAuditStatus, 0).eq(Poem::getDeleted, 0));
        created.put("poem", 0);
        skipped.put("poem", 0);
        for (Poem p : poems) {
            String type = "modern".equalsIgnoreCase(p.getPoemType()) ? "poem_modern" : "poem_ancient";
            String authorName = poetNames.get(p.getPoetId());
            if (!StringUtils.hasText(authorName)) {
                authorName = StringUtils.hasText(p.getPoetName()) ? p.getPoetName() : p.getAuthor();
            }
            AuditTicket t = newTicket("poem", p.getId())
                    .title(cut(p.getTitle(), 120))
                    .authorName(authorName)
                    .authorRef(p.getPoetId() == null ? null : String.valueOf(p.getPoetId()))
                    .dynastyName(p.getDynastyName())
                    .contentType(type)
                    .build();
            count(created, skipped, "poem", ensureTicket(t, existing), assignedHolder);
        }

        // 2) 评论
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 0).eq(Comment::getDeleted, 0));
        created.put("comment", 0);
        skipped.put("comment", 0);
        for (Comment c : comments) {
            String nick = c.getUsername();
            if (!StringUtils.hasText(nick) && c.getUserId() != null) {
                User u = userMapper.selectById(c.getUserId());
                nick = u == null ? null : u.getNickname();
            }
            AuditTicket t = newTicket("comment", c.getId())
                    .title(cut(c.getContent(), 120))
                    .authorName(nick)
                    .contentType("comment")
                    .build();
            count(created, skipped, "comment", ensureTicket(t, existing), assignedHolder);
        }

        // 3) AI 生成内容
        List<AiGeneratedContent> ais = aiGeneratedContentMapper.selectList(
                new LambdaQueryWrapper<AiGeneratedContent>().eq(AiGeneratedContent::getStatus, 0));
        created.put("ai_content", 0);
        skipped.put("ai_content", 0);
        for (AiGeneratedContent a : ais) {
            String title = (StringUtils.hasText(a.getTargetName()) ? a.getTargetName() : "内容")
                    + " · " + fieldLabel(a.getFieldName());
            AuditTicket t = newTicket("ai_content", a.getId())
                    .title(cut(title, 120))
                    .authorName(a.getTargetName())
                    .contentType("ai_content")
                    .build();
            count(created, skipped, "ai_content", ensureTicket(t, existing), assignedHolder);
        }

        // 4) 诗人认证（未认证与认证中都算待处理）
        List<PoetProfile> profiles = poetProfileMapper.selectList(new LambdaQueryWrapper<PoetProfile>()
                .in(PoetProfile::getVerifiedStatus, List.of(0, 2)).eq(PoetProfile::getDeleted, 0));
        created.put("poet_profile", 0);
        skipped.put("poet_profile", 0);
        for (PoetProfile p : profiles) {
            AuditTicket t = newTicket("poet_profile", p.getId())
                    .title(cut(p.getPenName() == null ? "诗人认证申请" : p.getPenName(), 120))
                    .authorName(p.getPenName())
                    .authorRef(p.getUserId() == null ? null : String.valueOf(p.getUserId()))
                    .contentType("poet_profile")
                    .build();
            count(created, skipped, "poet_profile", ensureTicket(t, existing), assignedHolder);
        }

        // 5) 正文修订草稿
        List<PoetDraft> drafts = poetDraftMapper.selectList(
                new LambdaQueryWrapper<PoetDraft>().eq(PoetDraft::getStatus, 0));
        created.put("poet_draft", 0);
        skipped.put("poet_draft", 0);
        for (PoetDraft d : drafts) {
            String poetName = poetNames.getOrDefault(d.getPoetId(), "诗人#" + d.getPoetId());
            AuditTicket t = newTicket("poet_draft", d.getId())
                    .title(cut(poetName + " · " + sectionLabel(d.getSection()), 120))
                    .authorName(poetName)
                    .authorRef(String.valueOf(d.getPoetId()))
                    .contentType("poet_draft")
                    .build();
            count(created, skipped, "poet_draft", ensureTicket(t, existing), assignedHolder);
        }

        // 6) 用户建议
        List<PoetSuggestion> suggestions = poetSuggestionMapper.selectList(
                new LambdaQueryWrapper<PoetSuggestion>().eq(PoetSuggestion::getStatus, "pending"));
        created.put("poet_suggestion", 0);
        skipped.put("poet_suggestion", 0);
        for (PoetSuggestion s : suggestions) {
            String poetName = poetNames.getOrDefault(s.getPoetId(), "诗人#" + s.getPoetId());
            AuditTicket t = newTicket("poet_suggestion", s.getId())
                    .title(cut(poetName + " · " + sectionLabel(s.getSection()), 120))
                    .authorName(poetName)
                    .authorRef(String.valueOf(s.getPoetId()))
                    .contentType("poet_suggestion")
                    .build();
            count(created, skipped, "poet_suggestion", ensureTicket(t, existing), assignedHolder);
        }

        // ensureTicket 建单时已顺手分派，这里只处理仍留在池里的
        int assigned = assignedHolder[0] + assignPending();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("createdTotal", created.values().stream().mapToInt(Integer::intValue).sum());
        result.put("assigned", assigned);
        return result;
    }

    private void count(Map<String, Integer> created, Map<String, Integer> skipped, String key, int code,
                       int[] assignedHolder) {
        if (code == 0) {
            skipped.merge(key, 1, Integer::sum);
        } else {
            created.merge(key, 1, Integer::sum);
            if (code == 2) {
                assignedHolder[0]++;
            }
        }
    }

    /** 处理一条待审数据：返回 0=已存在 1=新建未分派 2=新建已分派 */
    private int ensureTicket(AuditTicket t, Set<String> existing) {
        String key = t.getBizType() + "#" + t.getBizId();
        if (existing.contains(key)) {
            return 0;
        }
        t.setPriority(0);
        t.setStatus("pending");
        t.setCreateTime(LocalDateTime.now());
        t.setDeadlineAt(deadlineOf(0));
        ticketMapper.insert(t);
        existing.add(key);
        return applyAssign(t) ? 2 : 1;
    }

    private TicketBuilder newTicket(String bizType, Long bizId) {
        return new TicketBuilder(bizType, bizId);
    }

    private class TicketBuilder {
        private final AuditTicket t = new AuditTicket();

        TicketBuilder(String bizType, Long bizId) {
            t.setBizType(bizType);
            t.setBizId(bizId);
        }

        TicketBuilder title(String v) { t.setTitle(v); return this; }
        TicketBuilder authorName(String v) { t.setAuthorName(v); return this; }
        TicketBuilder authorRef(String v) { t.setAuthorRef(v); return this; }
        TicketBuilder dynastyName(String v) { t.setDynastyName(v); return this; }
        TicketBuilder contentType(String v) { t.setContentType(v); return this; }

        AuditTicket build() {
            return t;
        }
    }

    // ==================== 分派 ====================

    @Override
    @Transactional
    public int assignPending() {
        List<AuditTicket> pending = ticketMapper.selectList(
                new LambdaQueryWrapper<AuditTicket>().eq(AuditTicket::getStatus, "pending"));
        int assigned = 0;
        for (AuditTicket t : pending) {
            if (applyAssign(t)) {
                assigned++;
            }
        }
        return assigned;
    }

    /** 按规则分派；命中失败或超载时回落到「擅长+负载最小」 */
    private boolean applyAssign(AuditTicket t) {
        Pick pick = pickReviewer(t);
        if (pick == null) {
            return false;
        }
        t.setReviewerId(pick.reviewer.getId());
        t.setAssignNote(pick.note);
        t.setStatus("assigned");
        t.setAssignedAt(LocalDateTime.now());
        t.setDeadlineAt(deadlineOf(t.getPriority() == null ? 0 : t.getPriority()));
        ticketMapper.updateById(t);
        return true;
    }

    private Pick pickReviewer(AuditTicket t) {
        List<AuditRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<AuditRule>()
                .eq(AuditRule::getEnabled, 1)
                .orderByAsc(AuditRule::getPriority)
                .orderByAsc(AuditRule::getId));

        AuditRule globalRule = null;
        for (AuditRule rule : rules) {
            // global 不参与正常匹配，它只能做最后的兜底：
            // 否则「按擅长+负载均衡」永远轮不到，会把所有未命中内容全压给兜底人
            if ("global".equals(rule.getScopeType())) {
                globalRule = rule;
                continue;
            }
            if (!matches(rule, t)) {
                continue;
            }
            AuditReviewer rv = reviewerMapper.selectById(rule.getReviewerId());
            if (isAssignable(rv)) {
                return new Pick(rv, "规则：" + scopeText(rule));
            }
        }

        // 精确规则未命中或指定人已满：在擅长该类型、未超上限的审核员里选在手最少的
        List<AuditReviewer> candidates = reviewerMapper.selectList(
                new LambdaQueryWrapper<AuditReviewer>().eq(AuditReviewer::getStatus, 1));
        AuditReviewer best = null;
        long bestLoad = Long.MAX_VALUE;
        for (AuditReviewer rv : candidates) {
            if (!specializes(rv, t.getContentType()) || !isAssignable(rv)) {
                continue;
            }
            long load = loadOf(rv.getId());
            if (load < bestLoad) {
                best = rv;
                bestLoad = load;
            }
        }
        if (best != null) {
            return new Pick(best, "按擅长+负载分派（在手 " + bestLoad + " 单）");
        }

        // 最后才用全局兜底
        if (globalRule != null) {
            AuditReviewer rv = reviewerMapper.selectById(globalRule.getReviewerId());
            if (isAssignable(rv)) {
                return new Pick(rv, "全局兜底");
            }
        }
        return null;
    }

    /**
     * 自动派单只考虑「在岗 + 未超上限 + 角色为审核员」的账号。
     * 管理员是审核员的上级，可手动协审，但不进入自动派单池。
     */
    private boolean isAssignable(AuditReviewer rv) {
        if (!canTake(rv)) {
            return false;
        }
        User u = userMapper.selectById(rv.getUserId());
        return u != null && u.getStatus() != null && u.getStatus() == 1
                && "reviewer".equalsIgnoreCase(u.getRole());
    }

    private boolean matches(AuditRule rule, AuditTicket t) {
        String scope = rule.getScopeType();
        if (scope == null) {
            return false;
        }
        return switch (scope) {
            case "author" -> StringUtils.hasText(t.getAuthorRef())
                    && t.getAuthorRef().equals(rule.getScopeValue());
            case "dynasty" -> StringUtils.hasText(t.getDynastyName())
                    && t.getDynastyName().equals(rule.getScopeValue());
            case "content_type" -> StringUtils.hasText(t.getContentType())
                    && t.getContentType().equals(rule.getScopeValue());
            case "global" -> true;
            default -> false;
        };
    }

    private String scopeText(AuditRule rule) {
        return switch (rule.getScopeType()) {
            case "author" -> "作者 " + rule.getScopeValue();
            case "dynasty" -> "朝代 " + rule.getScopeValue();
            case "content_type" -> "类型 " + rule.getScopeValue();
            case "global" -> "全局兜底";
            default -> rule.getScopeType();
        };
    }

    private boolean canTake(AuditReviewer rv) {
        if (rv == null || rv.getStatus() == null || rv.getStatus() != 1) {
            return false;
        }
        int max = rv.getMaxConcurrent() == null ? 30 : rv.getMaxConcurrent();
        return loadOf(rv.getId()) < max;
    }

    private boolean specializes(AuditReviewer rv, String contentType) {
        String s = rv.getSpecialties();
        if (!StringUtils.hasText(s) || !StringUtils.hasText(contentType)) {
            return false;
        }
        for (String item : s.split(",")) {
            if (item.trim().equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    private long loadOf(Long reviewerId) {
        return ticketMapper.selectCount(new LambdaQueryWrapper<AuditTicket>()
                .eq(AuditTicket::getReviewerId, reviewerId)
                .in(AuditTicket::getStatus, ACTIVE_STATUS));
    }

    private LocalDateTime deadlineOf(int priority) {
        int hours = priority >= 2 ? 2 : priority == 1 ? 8 : 24;
        return LocalDateTime.now().plusHours(hours);
    }

    private record Pick(AuditReviewer reviewer, String note) {
    }

    // ==================== 流转 ====================

    @Override
    @Transactional
    public void claim(Long ticketId, Long currentUserId, boolean admin) {
        AuditTicket t = mustGet(ticketId);
        AuditReviewer reviewer = admin ? ensureReviewer(currentUserId) : registeredReviewer(currentUserId);
        // 管理员作为上级可协审任意未归档工单（含审核员处理中）；审核员只能认领未分派或分派给自己的单
        boolean claimable = admin
                ? !"approved".equals(t.getStatus()) && !"rejected".equals(t.getStatus())
                : "assigned".equals(t.getStatus()) || "pending".equals(t.getStatus());
        if (!claimable) {
            throw new BusinessException("该工单当前状态不可认领");
        }
        String previousReviewer = null;
        if (t.getReviewerId() != null && !reviewer.getId().equals(t.getReviewerId())) {
            if (!admin) {
                throw new BusinessException("该工单已分派给其他审核员");
            }
            previousReviewer = reviewerNameMap().get(t.getReviewerId());
        }
        t.setReviewerId(reviewer.getId());
        t.setStatus("reviewing");
        t.setClaimedAt(LocalDateTime.now());
        if (t.getAssignedAt() == null) {
            t.setAssignedAt(LocalDateTime.now());
        }
        t.setAssignNote(admin
                ? (StringUtils.hasText(previousReviewer) ? "管理员协审（接手 " + previousReviewer + "）" : "管理员协审认领")
                : "本人认领");
        ticketMapper.updateById(t);
    }

    @Override
    @Transactional
    public void transfer(Long ticketId, Long targetReviewerId, Long currentUserId, boolean admin) {
        AuditTicket t = mustGet(ticketId);
        requireOperateRight(t, currentUserId, admin, "转交");
        AuditReviewer target = reviewerMapper.selectById(targetReviewerId);
        User targetUser = target == null ? null : userMapper.selectById(target.getUserId());
        if (target == null || target.getStatus() == null || target.getStatus() != 1
                || targetUser == null || !"reviewer".equalsIgnoreCase(targetUser.getRole())) {
            throw new BusinessException("目标审核员不存在、已停用或不是审核员角色");
        }
        if ("approved".equals(t.getStatus()) || "rejected".equals(t.getStatus())) {
            throw new BusinessException("已归档的工单不能转交");
        }
        // 清空认领时间必须显式 set：MyBatis-Plus 默认策略会跳过 null 字段
        ticketMapper.update(null, new LambdaUpdateWrapper<AuditTicket>()
                .eq(AuditTicket::getId, ticketId)
                .set(AuditTicket::getReviewerId, targetReviewerId)
                .set(AuditTicket::getStatus, "assigned")
                .set(AuditTicket::getClaimedAt, null)
                .set(AuditTicket::getAssignedAt, LocalDateTime.now())
                .set(AuditTicket::getAssignNote, (admin ? "管理员改派给 " : "转交给 ") + target.getDisplayName()));
    }

    @Override
    @Transactional
    public void returnToPool(Long ticketId, Long currentUserId, boolean admin) {
        AuditTicket t = mustGet(ticketId);
        requireOperateRight(t, currentUserId, admin, "退回");
        if ("approved".equals(t.getStatus()) || "rejected".equals(t.getStatus())) {
            throw new BusinessException("已归档的工单不能退回");
        }
        ticketMapper.update(null, new LambdaUpdateWrapper<AuditTicket>()
                .eq(AuditTicket::getId, ticketId)
                .set(AuditTicket::getReviewerId, null)
                .set(AuditTicket::getStatus, "pending")
                .set(AuditTicket::getClaimedAt, null)
                .set(AuditTicket::getAssignedAt, null)
                .set(AuditTicket::getAssignNote, admin ? "管理员退回待分派" : "审核员退回待分派"));
    }

    /** 审核员只能操作自己的工单；管理员作为上级可以操作任意工单 */
    private void requireOperateRight(AuditTicket t, Long currentUserId, boolean admin, String action) {
        if (admin) {
            return;
        }
        AuditReviewer reviewer = registeredReviewer(currentUserId);
        if (t.getReviewerId() == null || !reviewer.getId().equals(t.getReviewerId())) {
            throw new BusinessException("该工单不属于你，无法" + action);
        }
    }

    private AuditReviewer registeredReviewer(Long userId) {
        AuditReviewer r = findReviewerOfUser(userId);
        if (r == null) {
            throw new BusinessException("当前账号未登记为审核员");
        }
        return r;
    }

    /** 管理员协审时按需补一条审核员档案，但它不会进入自动派单池（角色过滤） */
    private AuditReviewer ensureReviewer(Long userId) {
        AuditReviewer existing = findReviewerOfUser(userId);
        if (existing != null) {
            return existing;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("当前账号不存在");
        }
        AuditReviewer created = new AuditReviewer();
        created.setUserId(userId);
        created.setDisplayName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        created.setSpecialties("poem_ancient,poem_modern,comment,ai_content,poet_profile,poet_draft,poet_suggestion");
        created.setMaxConcurrent(100);
        created.setStatus(1);
        reviewerMapper.insert(created);
        return created;
    }

    @Override
    public void syncResult(String bizType, Long bizId, boolean approved, String reason) {
        // 弱管控：工单回写失败绝不能影响审核主流程，异常一律吞掉只记日志
        try {
            AuditTicket t = ticketMapper.selectOne(new LambdaQueryWrapper<AuditTicket>()
                    .eq(AuditTicket::getBizType, bizType)
                    .eq(AuditTicket::getBizId, bizId)
                    .last("limit 1"));
            if (t == null) {
                return;
            }
            t.setStatus(approved ? "approved" : "rejected");
            t.setResult(cut(reason, 500));
            t.setFinishedAt(LocalDateTime.now());
            if (t.getAssignedAt() == null) {
                t.setAssignedAt(LocalDateTime.now());
            }
            ticketMapper.updateById(t);
            notifyAuditResult(bizType, bizId, approved, reason);
        } catch (Exception e) {
            log.warn("工单状态回写失败 bizType={} bizId={}", bizType, bizId, e);
        }
    }

    /** 只给平台用户可识别的作者推站内通知；诗词/草稿等作者不是用户账号，跳过 */
    private void notifyAuditResult(String bizType, Long bizId, boolean approved, String reason) {
        Long targetUserId = null;
        String link = null;
        String label = "内容";
        if ("comment".equals(bizType)) {
            Comment comment = commentMapper.selectById(bizId);
            if (comment != null) {
                targetUserId = comment.getUserId();
                label = "评论";
                if (comment.getTargetType() != null) {
                    link = comment.getTargetType() == 2
                            ? "/forum/" + comment.getTargetId()
                            : "/poem/" + comment.getTargetId();
                }
            }
        } else if ("poet_profile".equals(bizType)) {
            PoetProfile profile = poetProfileMapper.selectById(bizId);
            if (profile != null) {
                targetUserId = profile.getUserId();
                label = "诗人认证申请";
                link = "/user/profile";
            }
        }
        if (targetUserId == null) {
            return;
        }
        notificationService.push(targetUserId, "audit_result",
                approved ? label + "审核通过" : label + "未通过审核",
                (approved ? "你提交的" + label + "已通过审核。" : "你提交的" + label + "未通过审核。")
                        + (StringUtils.hasText(reason) ? "原因：" + reason : ""),
                bizType, bizId, link);
    }

    private AuditTicket mustGet(Long id) {
        AuditTicket t = ticketMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("工单不存在");
        }
        return t;
    }

    // ==================== 规则与审核员 ====================

    @Override
    public List<Map<String, Object>> listRules() {
        Map<Long, String> names = reviewerNameMap();
        return ruleMapper.selectList(new LambdaQueryWrapper<AuditRule>()
                        .orderByAsc(AuditRule::getPriority).orderByAsc(AuditRule::getId))
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("scopeType", r.getScopeType());
                    m.put("scopeValue", r.getScopeValue());
                    m.put("reviewerId", r.getReviewerId());
                    m.put("reviewerName", names.get(r.getReviewerId()));
                    m.put("priority", r.getPriority());
                    m.put("enabled", r.getEnabled());
                    m.put("remark", r.getRemark());
                    return m;
                }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listReviewers() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<Long, User> users = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<AuditReviewer> reviewers = reviewerMapper.selectList(
                new LambdaQueryWrapper<AuditReviewer>().orderByAsc(AuditReviewer::getId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AuditReviewer rv : reviewers) {
            User user = users.get(rv.getUserId());
            // 只有 role=reviewer 的账号才是正式审核员；管理员协审档案不混入负载面板
            if (user == null || !"reviewer".equalsIgnoreCase(user.getRole())) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rv.getId());
            m.put("userId", rv.getUserId());
            m.put("username", user.getUsername());
            m.put("displayName", rv.getDisplayName());
            m.put("specialties", rv.getSpecialties());
            m.put("maxConcurrent", rv.getMaxConcurrent());
            m.put("status", rv.getStatus());

            List<AuditTicket> active = ticketMapper.selectList(new LambdaQueryWrapper<AuditTicket>()
                    .eq(AuditTicket::getReviewerId, rv.getId())
                    .in(AuditTicket::getStatus, ACTIVE_STATUS));
            m.put("onHand", active.size());
            m.put("overdue", active.stream().filter(this::isOverdue).count());
            m.put("doneToday", ticketMapper.selectCount(new LambdaQueryWrapper<AuditTicket>()
                    .eq(AuditTicket::getReviewerId, rv.getId())
                    .in(AuditTicket::getStatus, List.of("approved", "rejected"))
                    .ge(AuditTicket::getFinishedAt, todayStart)));
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listReviewerCandidates() {
        Set<Long> registered = reviewerMapper.selectList(null).stream()
                .map(AuditReviewer::getUserId)
                .collect(Collectors.toSet());
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "reviewer")
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId))
                .stream()
                .filter(u -> !registered.contains(u.getId()))
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", u.getId());
                    m.put("username", u.getUsername());
                    m.put("nickname", u.getNickname());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveReviewer(Long id, Long userId, String displayName, String specialties,
                             Integer maxConcurrent, Integer status) {
        AuditReviewer target;
        if (id == null) {
            if (userId == null) {
                throw new BusinessException("请选择要登记的审核员账号");
            }
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!"reviewer".equalsIgnoreCase(user.getRole())) {
                throw new BusinessException("该账号不是审核员角色，请先在用户管理中调整角色");
            }
            if (findReviewerOfUser(userId) != null) {
                throw new BusinessException("该账号已登记为审核员");
            }
            target = new AuditReviewer();
            target.setUserId(userId);
        } else {
            target = reviewerMapper.selectById(id);
            if (target == null) {
                throw new BusinessException("审核员档案不存在");
            }
        }
        if (StringUtils.hasText(displayName)) {
            target.setDisplayName(displayName.trim());
        } else if (id == null) {
            User user = userMapper.selectById(target.getUserId());
            target.setDisplayName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        }
        if (StringUtils.hasText(specialties)) {
            target.setSpecialties(specialties.trim());
        }
        if (maxConcurrent != null) {
            target.setMaxConcurrent(maxConcurrent < 1 ? 30 : maxConcurrent);
        } else if (id == null) {
            target.setMaxConcurrent(30);
        }
        if (status != null) {
            target.setStatus(status);
        } else if (id == null) {
            target.setStatus(1);
        }
        if (id == null) {
            reviewerMapper.insert(target);
        } else {
            reviewerMapper.updateById(target);
        }
    }

    @Override
    @Transactional
    public void deleteReviewer(Long id) {
        AuditReviewer reviewer = reviewerMapper.selectById(id);
        if (reviewer == null) {
            return;
        }
        long active = ticketMapper.selectCount(new LambdaQueryWrapper<AuditTicket>()
                .eq(AuditTicket::getReviewerId, id)
                .in(AuditTicket::getStatus, ACTIVE_STATUS));
        if (active > 0) {
            throw new BusinessException("该审核员还有 " + active + " 条在手工单，请先改派或停用");
        }
        // 其名下的分派规则一并清理，避免规则指向已删除的审核员
        ruleMapper.delete(new LambdaQueryWrapper<AuditRule>().eq(AuditRule::getReviewerId, id));
        reviewerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void saveRule(AuditRule rule) {
        if (!StringUtils.hasText(rule.getScopeType())) {
            throw new BusinessException("规则范围不能为空");
        }
        if (!"global".equals(rule.getScopeType()) && !StringUtils.hasText(rule.getScopeValue())) {
            throw new BusinessException("非全局规则必须指定匹配值");
        }
        if (rule.getReviewerId() == null) {
            throw new BusinessException("必须指定审核员");
        }
        if (rule.getPriority() == null) {
            rule.setPriority(defaultPriority(rule.getScopeType()));
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getId() == null) {
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
    }

    private int defaultPriority(String scopeType) {
        return switch (scopeType) {
            case "author" -> 10;
            case "dynasty" -> 20;
            case "content_type" -> 30;
            default -> 90;
        };
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    // ==================== 小工具 ====================

    private String cut(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() > max ? flat.substring(0, max) : flat;
    }

    private String fieldLabel(String field) {
        if (field == null) {
            return "内容";
        }
        return switch (field) {
            case "translation" -> "译文";
            case "appreciation" -> "赏析";
            case "background" -> "创作背景";
            case "biography" -> "生平简介";
            case "life_story" -> "人物生平";
            case "influence" -> "主要影响";
            case "evaluation" -> "历史评价";
            case "anecdotes" -> "轶事典故";
            default -> field;
        };
    }

    private String sectionLabel(String section) {
        if (section == null) {
            return "正文";
        }
        return switch (section) {
            case "biography", "introduction" -> "简介";
            case "life_story" -> "人物生平";
            case "influence" -> "主要影响";
            case "evaluation" -> "历史评价";
            case "anecdotes" -> "轶事典故";
            default -> section;
        };
    }
}
