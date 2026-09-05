package com.moyuan.ai;

import com.moyuan.entity.AiModel;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NvidiaAdapter implements AiModelAdapter {

    private final RestTemplate restTemplate;

    public NvidiaAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String chat(String message, AiModel model, String systemPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(model.getApiKey());

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getModelId());
        body.put("messages", messages);
        body.put("max_tokens", model.getMaxTokens() != null ? model.getMaxTokens() : 1024);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(model.getApiUrl(), request, (Class<Map<String, Object>>) (Class<?>) Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractContent(response.getBody());
            }
            throw new BusinessException(ResultCode.ERROR, "NVIDIA NIM调用失败");
        } catch (Exception e) {
            log.error("NVIDIA NIM调用失败, modelId: {}", model.getModelId(), e);
            throw new BusinessException(ResultCode.ERROR, "NVIDIA NIM调用失败: " + e.getMessage());
        }
    }

    @Override
    public String vision(String prompt, String base64Image, AiModel model, String systemPrompt) {
        // 当前无合适的 NVIDIA NIM 视觉模型：该供应商仅用于文本生成场景。
        // 完善流程：明确抛出可操作的错误，提示前端回退到支持视觉的模型（如智谱/千问的视觉模型）。
        throw new BusinessException(ResultCode.ERROR, "当前所选 NVIDIA NIM 模型不支持视觉识别，请改用支持视觉的模型（如智谱 glm-4v-flash 或千问视觉模型）");
    }

    @Override
    public boolean supports(String provider) {
        return "nvidia".equals(provider);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
            return (String) messageObj.get("content");
        }
        throw new BusinessException(ResultCode.ERROR, "无法解析NVIDIA NIM响应");
    }
}