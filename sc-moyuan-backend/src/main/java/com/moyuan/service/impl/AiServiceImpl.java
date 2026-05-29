package com.moyuan.service.impl;

import com.moyuan.config.AiConfig;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;

    @Override
    public String chat(String message, String model) {
        String apiKey;
        String baseUrl;

        switch (model.toLowerCase()) {
            case "zhipu":
                apiKey = aiConfig.getZhipu().getApi().getKey();
                baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
                return callZhipuApi(message, apiKey, baseUrl);
            case "deepseek":
                apiKey = aiConfig.getDeepseek().getApi().getApiKey();
                baseUrl = "https://api.deepseek.com/v1/chat/completions";
                return callDeepseekApi(message, apiKey, baseUrl);
            case "kimi":
                apiKey = aiConfig.getKimi().getApi().getApiKey();
                baseUrl = "https://api.moonshot.cn/v1/chat/completions";
                return callKimiApi(message, apiKey, baseUrl);
            case "mimo":
                apiKey = aiConfig.getMimo().getApi().getApiKey();
                baseUrl = "https://api.xiaomimimo.com/anthropic/v1/messages";
                return callMimoApi(message, apiKey, baseUrl);
            case "nvidia":
                apiKey = aiConfig.getNvidia().getApi().getApiKey();
                baseUrl = aiConfig.getNvidia().getApi().getBaseUrl();
                return callNvidiaApi(message, apiKey, baseUrl);
            case "glm-5":
                return callNvidiaModel(message, "zai-org/glm-5");
            case "glm-5.1":
                return callNvidiaModel(message, "zai-org/glm-51");
            case "deepseek-v4":
                return callNvidiaModel(message, "deepseek-ai/deepseek-v4-flash");
            case "kimi-k2.5":
                return callNvidiaModel(message, "moonshotai/kimi-k2.5");
            case "kimi-k2.6":
                return callNvidiaModel(message, "moonshotai/kimi-k2.6");
            case "minimax-m2.5":
                return callNvidiaModel(message, "minimax-ai/minimax-m25");
            case "qwen3.5":
                return callNvidiaModel(message, "qwen/qwen3.5-397b-a17b");
            case "llama-4-scout":
                return callNvidiaModel(message, "meta/llama-4-scout");
            case "llama-4-maverick":
                return callNvidiaModel(message, "meta/llama-4-maverick");
            default:
                throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的AI模型: " + model);
        }
    }

    private String callZhipuApi(String message, String apiKey, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "glm-4");
        body.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "智谱AI调用失败");
        } catch (Exception e) {
            log.error("智谱AI调用失败", e);
            throw new BusinessException(ResultCode.ERROR, "智谱AI调用失败: " + e.getMessage());
        }
    }

    private String callDeepseekApi(String message, String apiKey, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "DeepSeek调用失败");
        } catch (Exception e) {
            log.error("DeepSeek调用失败", e);
            throw new BusinessException(ResultCode.ERROR, "DeepSeek调用失败: " + e.getMessage());
        }
    }

    private String callKimiApi(String message, String apiKey, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "moonshot-v1-8k");
        body.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "Kimi调用失败");
        } catch (Exception e) {
            log.error("Kimi调用失败", e);
            throw new BusinessException(ResultCode.ERROR, "Kimi调用失败: " + e.getMessage());
        }
    }

    private String callMimoApi(String message, String apiKey, String baseUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-3-5-sonnet-20241022");
        body.put("max_tokens", 1024);
        body.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "MiMo调用失败");
        } catch (Exception e) {
            log.error("MiMo调用失败", e);
            throw new BusinessException(ResultCode.ERROR, "MiMo调用失败: " + e.getMessage());
        }
    }

    private String callNvidiaApi(String message, String apiKey, String baseUrl) {
        return callNvidiaModel(message, "meta/llama-4-scout");
    }

    private String callNvidiaModel(String message, String modelId) {
        String apiKey = aiConfig.getNvidia().getApi().getApiKey();
        String baseUrl = aiConfig.getNvidia().getApi().getBaseUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);
        body.put("messages", List.of(
                Map.of("role", "user", "content", message)
        ));
        body.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/chat/completions", request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "NVIDIA NIM调用失败");
        } catch (Exception e) {
            log.error("NVIDIA NIM调用失败, modelId: {}", modelId, e);
            throw new BusinessException(ResultCode.ERROR, "NVIDIA NIM调用失败: " + e.getMessage());
        }
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String prompt = "请根据这张图片创作一首古诗词。要求：1. 诗词要符合古诗词的格律和韵律；2. 内容要与图片意境相符；3. 请直接给出诗词内容，不需要额外解释。";

            if ("zhipu".equalsIgnoreCase(model)) {
                return callZhipuVisionApi(prompt, base64Image, aiConfig.getZhipu().getApi().getKey());
            } else {
                return chat("请根据以下描述创作一首古诗词：" + prompt, model);
            }
        } catch (IOException e) {
            log.error("图片处理失败", e);
            throw new BusinessException(ResultCode.ERROR, "图片处理失败: " + e.getMessage());
        }
    }

    @Override
    public String analyzePoem(String poem, String model) {
        String prompt = "请对以下古诗词进行详细分析，包括：\n" +
                "1. 诗词的意境和主题\n" +
                "2. 使用的修辞手法\n" +
                "3. 关键词句的赏析\n" +
                "4. 作者的情感表达\n" +
                "5. 诗词的历史背景（如果可能）\n\n" +
                "诗词内容：\n" + poem;
        return chat(prompt, model);
    }

    private String callZhipuVisionApi(String prompt, String base64Image, String apiKey) {
        String visionModel = aiConfig.getZhipu().getApi().getVisionModel();
        if (visionModel == null || visionModel.isEmpty()) {
            visionModel = "glm-4v-flash";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        content.put("text", prompt);

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        imageContent.put("image_url", Map.of("url", "data:image/jpeg;base64," + base64Image));

        Map<String, Object> body = new HashMap<>();
        body.put("model", visionModel);
        body.put("messages", List.of(
                Map.of("role", "user", "content", List.of(content, imageContent))
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://open.bigmodel.cn/api/paas/v4/chat/completions", request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            throw new BusinessException(ResultCode.ERROR, "智谱AI视觉模型调用失败");
        } catch (Exception e) {
            log.error("智谱AI视觉模型调用失败", e);
            throw new BusinessException(ResultCode.ERROR, "智谱AI视觉模型调用失败: " + e.getMessage());
        }
    }
}