package com.moyuan.ai;

import com.moyuan.entity.AiModel;

import java.util.List;
import java.util.Map;

public interface AiModelAdapter {

    String chat(String message, AiModel model, String systemPrompt);

    String vision(String prompt, String base64Image, AiModel model, String systemPrompt);

    boolean supports(String provider);

    /**
     * 流式对话。{@code messages} 为完整的 system + 历史 + user 数组。
     * <p>默认实现为降级：不支持流式的适配器一次性生成后将整段作为单个增量发出，
     * 保证前端仅需兼容「多个增量」与「整段一次到」两种形态。
     */
    default void streamChat(List<Map<String, Object>> messages, AiModel model,
                            Double temperature, Integer maxTokens, StreamCallback callback) {
        try {
            String systemPrompt = null;
            StringBuilder userContent = new StringBuilder();
            for (Map<String, Object> msg : messages) {
                Object content = msg.get("content");
                if (content == null) {
                    continue;
                }
                if ("system".equals(msg.get("role"))) {
                    systemPrompt = content.toString();
                } else {
                    userContent.append(content);
                }
            }
            String reply = chat(userContent.toString(), model, systemPrompt);
            if (callback.isCancelled()) {
                return;
            }
            callback.onDelta(reply);
            callback.onComplete();
        } catch (Throwable e) {
            callback.onError(e);
        }
    }
}
