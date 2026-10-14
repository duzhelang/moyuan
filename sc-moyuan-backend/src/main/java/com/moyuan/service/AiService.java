package com.moyuan.service;

import com.moyuan.ai.StreamCallback;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface AiService {

    String chat(String message, String model);

    String chat(String message, String model, String moduleCode);

    /**
     * 流式对话。{@code messages} 为完整的 system + 历史 + user 数组；
     * {@code moduleCode} 为空时回退默认模型；结果通过 {@link StreamCallback} 异步回调。
     */
    void streamChat(List<Map<String, Object>> messages, String moduleCode,
                    Double temperature, Integer maxTokens, StreamCallback callback);

    String writePoemFromImage(MultipartFile image, String model);

    String writePoemFromImage(MultipartFile image, String model, String visionModel);

    String writePoemFromImage(MultipartFile image, String model, String visionModel, String moduleCode);

    String analyzePoem(String poem, String model);

    String analyzePoem(String poem, String model, String moduleCode);

    String matchCouplet(String upperCouplet, String model);

    String matchCouplet(String upperCouplet, String model, String moduleCode);

    String ocrImage(MultipartFile image, String model, String visionModel, String moduleCode);
}
