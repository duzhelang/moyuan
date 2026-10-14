package com.moyuan.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.entity.AiModel;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Component
public class OpenAiCompatibleAdapter implements AiModelAdapter {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 共享单例 HttpClient，复用连接池，禁止每次调用新建 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OpenAiCompatibleAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String message, AiModel model, String systemPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(model.getApiKey());

        List<Map<String, String>> messages = buildMessages(systemPrompt, message);

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
            throw new BusinessException(ResultCode.ERROR, model.getDisplayName() + "调用失败");
        } catch (Exception e) {
            log.error("{}调用失败", model.getDisplayName(), e);
            throw new BusinessException(ResultCode.ERROR, model.getDisplayName() + "调用失败: " + e.getMessage());
        }
    }

    @Override
    public String vision(String prompt, String base64Image, AiModel model, String systemPrompt) {
        String visionModelId = model.getVisionModelId();
        if (visionModelId == null || visionModelId.isEmpty()) {
            throw new BusinessException(ResultCode.ERROR, model.getDisplayName() + "不支持视觉模型");
        }

        log.info("开始调用视觉模型: {}, provider: {}, apiUrl: {}", visionModelId, model.getProvider(), model.getApiUrl());
        log.info("图片base64长度: {}", base64Image != null ? base64Image.length() : 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(model.getApiKey());

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));

        List<Map<String, Object>> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", List.of(imageContent, textContent)));

        Map<String, Object> body = new HashMap<>();
        body.put("model", visionModelId);
        body.put("messages", messages);
        body.put("max_tokens", model.getMaxTokens() != null ? model.getMaxTokens() : 1024);
        // DeepSeek 官方视觉模型默认走思考模式，会导致 content 为空；此处显式关闭思考，保证 content 返回实际内容（其余厂商忽略该参数）
        if ("deepseek".equals(model.getProvider())) {
            body.put("thinking", Map.of("type", "disabled"));
        }

        log.info("视觉模型请求体: model={}, messages数量={}", visionModelId, messages.size());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(model.getApiUrl(), request, (Class<Map<String, Object>>) (Class<?>) Map.class);
            log.info("视觉模型响应状态: {}", response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("视觉模型响应成功");
                return extractContent(response.getBody());
            }
            log.error("视觉模型响应失败: status={}, body={}", response.getStatusCode(), response.getBody());
            throw new BusinessException(ResultCode.ERROR, model.getDisplayName() + "视觉模型调用失败");
        } catch (Exception e) {
            log.error("{}视觉模型调用失败, apiUrl={}, model={}", model.getDisplayName(), model.getApiUrl(), visionModelId, e);
            throw new BusinessException(ResultCode.ERROR, model.getDisplayName() + "视觉模型调用失败: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String provider) {
        return "zhipu".equals(provider) || "deepseek".equals(provider) || "kimi".equals(provider) || "mimo".equals(provider)
                || "qwen".equals(provider) || "openrouter".equals(provider);
    }

    @Override
    public void streamChat(List<Map<String, Object>> messages, AiModel model,
                           Double temperature, Integer maxTokens, StreamCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model.getModelId());
        body.put("messages", messages);
        body.put("max_tokens", maxTokens != null ? maxTokens
                : (model.getMaxTokens() != null ? model.getMaxTokens() : 1024));
        body.put("stream", true);
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        // DeepSeek 默认走思考模式会导致 content 为空；显式关闭思考，保证增量可见（其余厂商忽略该参数）
        if ("deepseek".equals(model.getProvider())) {
            body.put("thinking", Map.of("type", "disabled"));
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(model.getApiUrl()))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + model.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            callback.onError(e);
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .whenComplete((resp, err) -> {
                    if (err != null) {
                        callback.onError(err);
                        return;
                    }
                    if (resp.statusCode() / 100 != 2) {
                        callback.onError(new BusinessException(ResultCode.ERROR,
                                model.getDisplayName() + "流式调用失败: HTTP " + resp.statusCode()));
                        return;
                    }
                    try (Stream<String> lines = resp.body()) {
                        Iterator<String> it = lines.iterator();
                        while (it.hasNext()) {
                            if (callback.isCancelled()) {
                                return; // 客户端断开，停止读取上游
                            }
                            String delta = SseParser.extractDelta(it.next());
                            if (delta == null) {
                                continue; // 空行 / 非 data 行 / role 块
                            }
                            if (delta.isEmpty()) {
                                callback.onComplete(); // data: [DONE]
                                return;
                            }
                            callback.onDelta(delta);
                        }
                        callback.onComplete();
                    } catch (Throwable e) {
                        callback.onError(e);
                    }
                });
    }

    private List<Map<String, String>> buildMessages(String systemPrompt, String userMessage) {
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
            return (String) messageObj.get("content");
        }
        throw new BusinessException(ResultCode.ERROR, "无法解析AI响应");
    }
}