package com.moyuan.util;

import com.moyuan.common.ResultCode;
import com.moyuan.exception.BusinessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 对外接口的入参校验与 token 估算工具。
 * <p>
 * 只做「防误用/防滥用」级别的限制，不做业务语义校验；所有上限集中在此，便于调整。
 */
public final class AiInputValidator {

    /** 单轮问答最大字符数 */
    public static final int MAX_CHAT_CHARS = 4000;
    /** 智能分析最大字符数 */
    public static final int MAX_ANALYZE_CHARS = 6000;
    /** 对联上联最大字符数 */
    public static final int MAX_COUPLET_CHARS = 200;
    /** 平仄校验最大字符数 */
    public static final int MAX_TONE_CHARS = 2000;

    /** 流式对话最多消息条数（含 system） */
    public static final int MAX_STREAM_MESSAGES = 30;
    /** 流式对话单条消息最大字符数 */
    public static final int MAX_STREAM_MESSAGE_CHARS = 4000;
    /** 流式对话全部消息合计最大字符数 */
    public static final int MAX_STREAM_TOTAL_CHARS = 12000;
    /** 流式对话单次最大生成 token 数 */
    public static final int MAX_STREAM_TOKENS = 2048;

    private static final Set<String> ALLOWED_ROLES = Set.of("system", "user", "assistant");

    private AiInputValidator() {
    }

    /** 必填文本：去首尾空白后校验非空与长度上限 */
    public static String requireText(String value, String field, int maxChars) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, field + "不能为空");
        }
        String text = value.trim();
        if (text.length() > maxChars) {
            throw new BusinessException(ResultCode.AI_INPUT_TOO_LONG,
                    field + "不能超过 " + maxChars + " 字");
        }
        return text;
    }

    /**
     * 校验并归一化流式对话消息：只保留 role/content，限制条数与总长度，拒绝非法角色。
     */
    public static List<Map<String, Object>> requireMessages(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "messages 不能为空");
        }
        if (messages.size() > MAX_STREAM_MESSAGES) {
            throw new BusinessException(ResultCode.AI_INPUT_TOO_LONG,
                    "messages 最多 " + MAX_STREAM_MESSAGES + " 条");
        }

        List<Map<String, Object>> normalized = new ArrayList<>(messages.size());
        int totalChars = 0;
        int systemCount = 0;
        boolean hasUser = false;

        for (Map<String, Object> item : messages) {
            if (item == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "messages 存在空消息");
            }
            Object roleValue = item.get("role");
            Object contentValue = item.get("content");
            String role = roleValue instanceof String r ? r.trim().toLowerCase() : "";
            if (!ALLOWED_ROLES.contains(role)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的消息角色: " + role);
            }
            if (!(contentValue instanceof String content) || content.trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "消息内容不能为空");
            }
            String text = content.trim();
            if (text.length() > MAX_STREAM_MESSAGE_CHARS) {
                throw new BusinessException(ResultCode.AI_INPUT_TOO_LONG,
                        "单条消息不能超过 " + MAX_STREAM_MESSAGE_CHARS + " 字");
            }
            if ("system".equals(role) && ++systemCount > 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "system 消息最多 1 条");
            }
            if ("user".equals(role)) {
                hasUser = true;
            }
            totalChars += text.length();
            if (totalChars > MAX_STREAM_TOTAL_CHARS) {
                throw new BusinessException(ResultCode.AI_INPUT_TOO_LONG,
                        "对话内容不能超过 " + MAX_STREAM_TOTAL_CHARS + " 字");
            }
            normalized.add(Map.of("role", role, "content", text));
        }

        if (!hasUser) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "messages 至少包含 1 条 user 消息");
        }
        return normalized;
    }

    /** 校验并返回可用的 maxTokens；null 表示沿用模型配置 */
    public static Integer normalizeMaxTokens(Integer maxTokens) {
        if (maxTokens == null) {
            return null;
        }
        if (maxTokens <= 0 || maxTokens > MAX_STREAM_TOKENS) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "maxTokens 需在 1~" + MAX_STREAM_TOKENS + " 之间");
        }
        return maxTokens;
    }

    /** 校验温度；null 表示沿用模型默认值 */
    public static Double normalizeTemperature(Double temperature) {
        if (temperature == null) {
            return null;
        }
        if (temperature < 0 || temperature > 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "temperature 需在 0~2 之间");
        }
        return temperature;
    }

    /** 按字符数粗略估算 token：汉字约 1 token/字，其他字符约 3 字符/token */
    public static long estimateTokens(String... texts) {
        if (texts == null) {
            return 0;
        }
        long cjk = 0;
        long other = 0;
        for (String text : texts) {
            if (text == null) {
                continue;
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= '\u4E00' && c <= '\u9FFF') {
                    cjk++;
                } else if (!Character.isWhitespace(c)) {
                    other++;
                }
            }
        }
        return Math.max(1, (long) Math.ceil(cjk + other / 3.0));
    }

    /** 按字符数估算 token（用于流式输出增量累计） */
    public static long estimateTokensByChars(long chars) {
        return chars <= 0 ? 0 : Math.max(1, (long) Math.ceil(chars / 1.5));
    }
}
