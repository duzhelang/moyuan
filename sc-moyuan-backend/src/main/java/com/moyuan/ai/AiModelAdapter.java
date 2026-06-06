package com.moyuan.ai;

import com.moyuan.entity.AiModel;

public interface AiModelAdapter {

    String chat(String message, AiModel model, String systemPrompt);

    String vision(String prompt, String base64Image, AiModel model, String systemPrompt);

    boolean supports(String provider);
}