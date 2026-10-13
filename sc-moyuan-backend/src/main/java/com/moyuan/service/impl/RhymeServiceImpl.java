package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.entity.Rhyme;
import com.moyuan.mapper.RhymeMapper;
import com.moyuan.service.AiService;
import com.moyuan.service.RhymeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 韵脚查询服务：数据库优先，未命中时调用大模型现查并落库，后续同输入直接命中库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RhymeServiceImpl implements RhymeService {

    private final RhymeMapper rhymeMapper;

    /** 复用现有 AI 服务；moduleCode=chat 已在 ai_module_config 绑定 DeepSeek，避免落回智谱默认值 */
    private final AiService aiService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** AI 返回同韵字的数量上限，防止单次落库过多 */
    private static final int MAX_CHARS = 40;

    @Override
    public List<Rhyme> queryByCharacter(String character) {
        // 1. 先查库：命中则返回该字所属韵部的全部同韵字
        List<Rhyme> hits = dbByCharacter(character);
        if (!hits.isEmpty()) {
            String group = hits.get(0).getRhymeGroup();
            if (group != null && !group.trim().isEmpty()) {
                List<Rhyme> groupRows = dbByRhymeGroup(group);
                if (!groupRows.isEmpty()) {
                    return groupRows;
                }
            }
            return hits;
        }
        // 2. 未命中：AI 兜底现查 → 解析 → 落库 → 回查
        return aiFallbackByCharacter(character);
    }

    @Override
    public List<Rhyme> queryByRhymeGroup(String rhymeGroup) {
        List<Rhyme> rows = dbByRhymeGroup(rhymeGroup);
        if (!rows.isEmpty()) {
            return rows;
        }
        return aiFallbackByGroup(rhymeGroup);
    }

    /** 库内按汉字精确查询 */
    private List<Rhyme> dbByCharacter(String character) {
        return rhymeMapper.selectList(
                new LambdaQueryWrapper<Rhyme>()
                        .eq(Rhyme::getCharacter, character)
                        .orderByAsc(Rhyme::getSortOrder)
        );
    }

    /** 库内按韵部查询 */
    private List<Rhyme> dbByRhymeGroup(String rhymeGroup) {
        return rhymeMapper.selectList(
                new LambdaQueryWrapper<Rhyme>()
                        .eq(Rhyme::getRhymeGroup, rhymeGroup)
                        .orderByAsc(Rhyme::getSortOrder)
        );
    }

    /** 按汉字的 AI 兜底：查该字所属韵部及同韵字，落库后回查该韵部 */
    private List<Rhyme> aiFallbackByCharacter(String character) {
        String prompt = "你是一位精通《平水韵》的古典诗词专家。请判断汉字「" + character + "」在平水韵中的归属，"
                + "并给出该韵部的常用同韵字。\n"
                + "要求：只输出如下严格 JSON，不要任何解释文字、不要 markdown 代码块标记：\n"
                + "{\"rhymeGroup\":\"韵部名称，单个汉字，如 东、冬、江、支\","
                + "\"toneType\":\"平声 或 上声 或 去声 或 入声\","
                + "\"rhymeCategory\":\"平声填 上平/下平；仄声填 上声/去声/入声\","
                + "\"chars\":[\"同韵字\",\"同韵字\"]}\n"
                + "注意：chars 必须包含所查汉字本身，最多 " + MAX_CHARS + " 个字，每项仅一个汉字。";

        RhymeAiResult ai = callAi(prompt, "字=" + character);
        if (ai == null || ai.rhymeGroup == null || ai.rhymeGroup.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String group = ai.rhymeGroup.trim();
        String tone = normalizeTone(ai.toneType);
        String category = normalizeCategory(ai.rhymeCategory, tone);
        List<String> chars = normalizeChars(ai.chars);
        if (!chars.contains(character)) {
            chars.add(0, character);
        }
        for (int i = 0; i < chars.size(); i++) {
            insertRhyme(chars.get(i), group, tone, category, i + 1);
        }
        return dbByRhymeGroup(group);
    }

    /** 按韵部的 AI 兜底：查该韵部同韵字，落库后回查 */
    private List<Rhyme> aiFallbackByGroup(String rhymeGroup) {
        String prompt = "你是一位精通《平水韵》的古典诗词专家。请列出平水韵韵部「" + rhymeGroup + "」的常用同韵字。\n"
                + "要求：只输出如下严格 JSON，不要任何解释文字、不要 markdown 代码块标记：\n"
                + "{\"toneType\":\"平声 或 上声 或 去声 或 入声\","
                + "\"rhymeCategory\":\"平声填 上平/下平；仄声填 上声/去声/入声\","
                + "\"chars\":[\"同韵字\",\"同韵字\"]}\n"
                + "注意：最多 " + MAX_CHARS + " 个字，每项仅一个汉字。";

        RhymeAiResult ai = callAi(prompt, "韵部=" + rhymeGroup);
        if (ai == null) {
            return Collections.emptyList();
        }
        String tone = normalizeTone(ai.toneType);
        String category = normalizeCategory(ai.rhymeCategory, tone);
        List<String> chars = normalizeChars(ai.chars);
        if (chars.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < chars.size(); i++) {
            insertRhyme(chars.get(i), rhymeGroup, tone, category, i + 1);
        }
        return dbByRhymeGroup(rhymeGroup);
    }

    /** 调用大模型并解析为结构化结果；任何异常都返回 null，不向上抛 */
    private RhymeAiResult callAi(String prompt, String tag) {
        String raw;
        try {
            raw = aiService.chat(prompt, "zhipu", "chat");
        } catch (Exception e) {
            log.warn("AI 查询韵脚失败（{}）：{}", tag, e.getMessage());
            return null;
        }
        RhymeAiResult result = parseAiJson(raw);
        if (result == null) {
            log.warn("AI 返回内容无法解析为韵脚数据（{}）：{}", tag, raw);
        }
        return result;
    }

    /** 解析 AI 返回的 JSON：剥离 ``` 围栏、截取首个 {...} 片段 */
    private RhymeAiResult parseAiJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
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
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        String json = text.substring(start, end + 1);
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            RhymeAiResult result = new RhymeAiResult();
            result.rhymeGroup = node.hasNonNull("rhymeGroup") ? node.get("rhymeGroup").asText() : null;
            result.toneType = node.hasNonNull("toneType") ? node.get("toneType").asText() : null;
            result.rhymeCategory = node.hasNonNull("rhymeCategory") ? node.get("rhymeCategory").asText() : null;
            result.chars = new ArrayList<>();
            if (node.has("chars") && node.get("chars").isArray()) {
                for (JsonNode item : node.get("chars")) {
                    result.chars.add(item.asText(""));
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 AI 韵脚 JSON 失败：{}", e.getMessage());
            return null;
        }
    }

    /** 归一化同韵字：去空白、仅保留单个汉字、去重、限长 */
    private List<String> normalizeChars(List<String> chars) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (chars != null) {
            for (String c : chars) {
                if (c == null) {
                    continue;
                }
                String s = c.trim();
                if (s.codePointCount(0, s.length()) == 1) {
                    set.add(s);
                }
                if (set.size() >= MAX_CHARS) {
                    break;
                }
            }
        }
        return new ArrayList<>(set);
    }

    /** 归一化声调，缺省按平声处理 */
    private String normalizeTone(String tone) {
        if (tone == null || tone.trim().isEmpty()) {
            return "平声";
        }
        String t = tone.trim();
        if (t.contains("入")) {
            return "入声";
        }
        if (t.contains("去")) {
            return "去声";
        }
        if (t.contains("上")) {
            return "上声";
        }
        return "平声";
    }

    /** 归一化韵目分类，缺省时按声调推导 */
    private String normalizeCategory(String category, String tone) {
        if (category != null && !category.trim().isEmpty()) {
            return category.trim();
        }
        return "平声".equals(tone) ? "上平" : tone;
    }

    /** 逐字落库，失败仅告警不影响整体返回 */
    private void insertRhyme(String ch, String group, String tone, String category, int order) {
        Rhyme rhyme = new Rhyme();
        rhyme.setCharacter(ch);
        rhyme.setRhymeGroup(group);
        rhyme.setToneType(tone);
        rhyme.setRhymeCategory(category);
        rhyme.setSortOrder(order);
        try {
            rhymeMapper.insertIgnore(rhyme);
        } catch (Exception e) {
            log.warn("韵脚落库失败（字={}，韵部={}）：{}", ch, group, e.getMessage());
        }
    }

    /** AI 结果承载（仅内部使用） */
    private static class RhymeAiResult {
        private String rhymeGroup;
        private String toneType;
        private String rhymeCategory;
        private List<String> chars;
    }
}
