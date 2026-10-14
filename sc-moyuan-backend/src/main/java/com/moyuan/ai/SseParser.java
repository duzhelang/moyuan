package com.moyuan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI 兼容接口的 SSE 逐行解析器。
 *
 * <p>解析约定（返回值语义）：
 * <ul>
 *   <li>{@code null}：该行无有效增量（空行、注释行、非 data 行、role 块、解析失败），调用方应跳过；</li>
 *   <li>空串：收到 {@code data: [DONE]} 结束信号；</li>
 *   <li>其余：实际的增量文本。</li>
 * </ul>
 */
@Slf4j
public final class SseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SseParser() {
    }

    public static String extractDelta(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null; // 空行（事件分隔）
        }
        if (trimmed.startsWith(":")) {
            return null; // 注释 / 心跳行
        }
        if (!trimmed.startsWith("data:")) {
            return null; // event: / id: / retry: 等
        }

        String data = trimmed.substring("data:".length()).trim();
        if (data.isEmpty()) {
            return null;
        }
        if ("[DONE]".equals(data)) {
            return "";
        }

        try {
            JsonNode root = MAPPER.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).get("delta");
            if (delta == null) {
                return null;
            }
            JsonNode content = delta.get("content");
            if (content == null || content.isNull()) {
                return null; // 首个仅含 role 的块
            }
            String text = content.asText();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            // 单行解析失败不中断整条流，仅跳过
            log.debug("SSE 增量解析失败，跳过该行: {}", data);
            return null;
        }
    }
}
