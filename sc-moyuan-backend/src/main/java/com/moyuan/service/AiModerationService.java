package com.moyuan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.ai.AiModelRegistry;
import com.moyuan.entity.AiModuleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 发布内容时的 AI 自动风控。
 * 结论只作为辅助信号：命中中/高风险时生成 reporterId=0 的举报单，
 * 高风险由调用方同时限制内容可见性；任何模型异常都降级为低风险，绝不阻断发布。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationService {

    public static final String MODULE_CODE = "content_moderation";

    private static final Set<String> RISK_LEVELS = Set.of("low", "medium", "high");
    private static final int MAX_CONTENT_LENGTH = 2000;

    private static final String SYSTEM_PROMPT =
            "你是内容安全审核员。请判断用户发布的内容是否存在违规风险，只输出一个 JSON 对象，不要输出任何解释、前缀或 Markdown 代码块。\n"
            + "JSON 字段：\n"
            + "- riskLevel：low / medium / high（high 表示明显违法违规或严重有害）\n"
            + "- categories：字符串数组，可选值 spam、abuse、porn、plagiarism、illegal、politics、other\n"
            + "- reason：一句话中文说明判定依据，不超过 80 字\n"
            + "无风险时 riskLevel=low 且 categories=[]。";

    private final AiService aiService;
    private final AiModelRegistry aiModelRegistry;
    private final ReportService reportService;
    private final ObjectMapper objectMapper;

    /** 分析内容风险；失败时降级为低风险 */
    public ModerationResult analyze(String contentType, String title, String content) {
        if (!StringUtils.hasText(content)) {
            return ModerationResult.low();
        }
        try {
            String raw = callModel(buildPayload(contentType, title, content));
            return parse(raw);
        } catch (Exception e) {
            log.warn("AI 内容风控分析失败，降级为低风险 contentType={}", contentType, e);
            return ModerationResult.low();
        }
    }

    /** 命中中/高风险时自动建举报单（reporterId=0），低风险直接忽略 */
    public void handleRisk(String targetType, Long targetId, Long targetUserId, ModerationResult result) {
        if (result == null || !result.shouldReport()) {
            return;
        }
        reportService.createAiReport(targetType, targetId, targetUserId,
                result.riskLevel(), result.reason());
    }

    private String callModel(String payload) {
        AiModuleConfig config = aiModelRegistry.getModuleConfig(MODULE_CODE);
        if (config != null) {
            return aiService.chat(payload, null, MODULE_CODE);
        }
        // 模块配置尚未加载时，用默认模型 + 内置风控提示词兜底
        return aiModelRegistry.chat(payload, null, SYSTEM_PROMPT);
    }

    private String buildPayload(String contentType, String title, String content) {
        String type = switch (contentType == null ? "" : contentType) {
            case "post" -> "论坛帖子";
            case "comment" -> "评论";
            default -> "用户内容";
        };
        String body = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH) : content;
        return "内容类型：" + type
                + (StringUtils.hasText(title) ? "\n标题：" + title : "")
                + "\n正文：\n<<<\n" + body + "\n>>>";
    }

    private ModerationResult parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            return ModerationResult.low();
        }
        try {
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);
            String level = node.path("riskLevel").asText("low").trim().toLowerCase(Locale.ROOT);
            if (!RISK_LEVELS.contains(level)) {
                level = "low";
            }
            List<String> categories = new ArrayList<>();
            JsonNode cats = node.path("categories");
            if (cats.isArray()) {
                cats.forEach(item -> {
                    if (StringUtils.hasText(item.asText())) {
                        categories.add(item.asText());
                    }
                });
            }
            String reason = node.path("reason").asText("");
            if (reason.length() > 300) {
                reason = reason.substring(0, 300);
            }
            return new ModerationResult(level, categories, reason);
        } catch (Exception e) {
            log.warn("AI 风控结果解析失败，降级为低风险 raw={}", raw, e);
            return ModerationResult.low();
        }
    }

    /** 兼容模型输出 ```json 包裹或在 JSON 前后附带说明的情况 */
    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    public record ModerationResult(String riskLevel, List<String> categories, String reason) {

        public static ModerationResult low() {
            return new ModerationResult("low", List.of(), "");
        }

        public boolean shouldReport() {
            return "medium".equals(riskLevel) || "high".equals(riskLevel);
        }

        public boolean isHigh() {
            return "high".equals(riskLevel);
        }
    }
}
