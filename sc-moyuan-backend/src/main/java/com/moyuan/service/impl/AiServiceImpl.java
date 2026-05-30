package com.moyuan.service.impl;

import com.moyuan.ai.AiModelRegistry;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final AiModelRegistry aiModelRegistry;

    @Override
    public String chat(String message, String model) {
        return aiModelRegistry.chat(message, model);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model) {
        return writePoemFromImage(image, model, null);
    }

    @Override
    public String writePoemFromImage(MultipartFile image, String model, String visionModel) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String prompt = "请根据这张图片创作一首古诗词。要求：1. 诗词要符合古诗词的格律和韵律；2. 内容要与图片意境相符；3. 请直接给出诗词内容，不需要额外解释。";
            return aiModelRegistry.vision(prompt, base64Image, model, visionModel);
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
}