package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.dto.game.GameConstants;
import com.moyuan.dto.response.SentenceRef;
import com.moyuan.entity.Poem;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.AiService;
import com.moyuan.service.GameVerifyService;
import com.moyuan.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 三级真实性校验链（本地库 → APIHZ → LLM 兜底）+ 本地规则工具 + AI JSON 解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameVerifyServiceImpl implements GameVerifyService {

    private final PoemMapper poemMapper;
    private final RecommendationService recommendationService;
    private final AiService aiService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int EXTERNAL_LIMIT = 5;
    private static final int LOCAL_SCAN_LIMIT = 10;

    @Override
    public boolean verifyReal(String sentence) {
        return verifyReal(sentence, null, null);
    }

    @Override
    public boolean verifyReal(String sentence, Long dynastyId, String dynastyName) {
        String normalized = normalize(sentence);
        if (normalized.isEmpty()) {
            return false;
        }
        // ① 本地诗词库全文检索（最快、零网络、零 token）
        if (hitLocalLibrary(normalized, dynastyId)) {
            return true;
        }
        // ② 外部诗词接口复核：仅在不限朝代时可用（外部源无法确认朝代归属）
        if (dynastyId == null && hitExternalApi(normalized)) {
            return true;
        }
        // ③ LLM 兜底真伪判断（限定时附带朝代约束）
        return llmVerdict(normalized, dynastyName);
    }

    @Override
    public boolean verifyTopic(String sentence, String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return false;
        }
        try {
            String prompt = "主题：" + topic.trim() + "\n待校验古诗句子：" + normalize(sentence)
                    + "\n判断诗句是否贴合该主题，输出纯JSON。";
            String raw = aiService.chat(prompt, GameConstants.MODEL_FALLBACK, GameConstants.MODULE_TOPIC);
            return parseBooleanField(raw, "matchResult");
        } catch (Exception e) {
            log.warn("主题语义匹配失败：sentence={}，topic={}，error={}", sentence, topic, e.getMessage());
            return false;
        }
    }

    @Override
    public List<SentenceRef> parseCandidates(String raw) {
        List<SentenceRef> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String text = stripFence(raw);
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return result;
        }
        String json = text.substring(start, end + 1);
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            if (!node.isArray()) {
                return result;
            }
            for (JsonNode item : node) {
                String sentence = item.hasNonNull("sentence") ? item.get("sentence").asText("") : "";
                if (sentence == null || sentence.trim().isEmpty()) {
                    continue;
                }
                SentenceRef ref = new SentenceRef();
                ref.setSentence(sentence.trim());
                ref.setTitle(item.hasNonNull("title") ? item.get("title").asText("") : null);
                ref.setAuthor(item.hasNonNull("author") ? item.get("author").asText("") : null);
                ref.setMatchDesc(item.hasNonNull("matchDesc") ? item.get("matchDesc").asText("") : null);
                result.add(ref);
            }
        } catch (Exception e) {
            log.warn("解析 AI 候选诗句 JSON 失败：{}", e.getMessage());
        }
        return result;
    }

    @Override
    public String normalize(String sentence) {
        if (sentence == null) {
            return "";
        }
        return sentence.replaceAll("[^\\u4e00-\\u9fff]", "");
    }

    @Override
    public int hanziCount(String sentence) {
        return normalize(sentence).length();
    }

    @Override
    public int indexOfHanzi(String sentence, char target) {
        int idx = normalize(sentence).indexOf(target);
        return idx < 0 ? -1 : idx + 1;
    }

    @Override
    public char firstHanzi(String sentence) {
        String normalized = normalize(sentence);
        return normalized.isEmpty() ? 0 : normalized.charAt(0);
    }

    @Override
    public char lastHanzi(String sentence) {
        String normalized = normalize(sentence);
        return normalized.isEmpty() ? 0 : normalized.charAt(normalized.length() - 1);
    }

    /* ==================== 内部实现 ==================== */

    /**
     * 第一级：本地 poem 表 ngram 全文检索（status=1 且 poem_type='classical'）。
     * 取回候选后仍需正文实际包含该句，避免全文检索误命中自创句。
     * dynastyId 非空时仅在该朝代范围内检索。
     */
    private boolean hitLocalLibrary(String normalized, Long dynastyId) {
        try {
            LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Poem::getStatus, 1)
                    .eq(Poem::getPoemType, "classical")
                    .eq(dynastyId != null, Poem::getDynastyId, dynastyId)
                    .apply("MATCH(`title`, `content`) AGAINST ({0} IN NATURAL LANGUAGE MODE)", normalized)
                    .last("LIMIT " + LOCAL_SCAN_LIMIT);
            List<Poem> poems = poemMapper.selectList(wrapper);
            for (Poem poem : poems) {
                if (poem.getContent() != null && normalize(poem.getContent()).contains(normalized)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("本地库检索失败，降级下一级：sentence={}，error={}", normalized, e.getMessage());
        }
        return false;
    }

    /**
     * 第二级：外部诗词接口复核（collect=false，不触发静默收录）。
     */
    private boolean hitExternalApi(String normalized) {
        try {
            List<Map<String, Object>> results = recommendationService.getExternalPoems(normalized, EXTERNAL_LIMIT, false);
            if (results == null) {
                return false;
            }
            for (Map<String, Object> item : results) {
                Object content = item.get("content");
                if (content != null && normalize(String.valueOf(content)).contains(normalized)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("外部诗词接口复核失败，降级 LLM：sentence={}，error={}", normalized, e.getMessage());
        }
        return false;
    }

    /**
     * 第三级：LLM 兜底判断真伪；解析失败或不确认为真一律视为不通过。
     * dynastyName 非空时要求该句必须属于指定朝代的真实诗词，否则判为不通过。
     */
    private boolean llmVerdict(String normalized, String dynastyName) {
        try {
            String prompt;
            if (dynastyName != null && !dynastyName.trim().isEmpty()) {
                prompt = "需要核验的诗句：" + normalized + "\n请判断该句是否为「" + dynastyName.trim()
                        + "」朝代的真实诗词原句（属其他朝代或非真实诗句一律不算），输出纯JSON。";
            } else {
                prompt = "需要核验的诗句：" + normalized + "\n请判断该句是否为古代真实诗词原句，输出纯JSON。";
            }
            String raw = aiService.chat(prompt, GameConstants.MODEL_FALLBACK, GameConstants.MODULE_VERIFY);
            return parseBooleanField(raw, "isReal");
        } catch (Exception e) {
            log.warn("LLM 真伪核验失败：sentence={}，error={}", normalized, e.getMessage());
            return false;
        }
    }

    private boolean parseBooleanField(String raw, String field) {
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        String text = stripFence(raw);
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(text.substring(start, end + 1));
            return node.hasNonNull(field) && node.get(field).asBoolean(false);
        } catch (Exception e) {
            log.warn("解析 AI 布尔字段失败：field={}，error={}", field, e.getMessage());
            return false;
        }
    }

    /** 剥离 ``` 围栏，保留正文。 */
    private String stripFence(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            int nl = text.indexOf('\n');
            if (nl >= 0) {
                text = text.substring(nl + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
        }
        return text.trim();
    }
}
