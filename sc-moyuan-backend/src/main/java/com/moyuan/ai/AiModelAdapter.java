package com.moyuan.ai;

import com.moyuan.entity.AiModel;

public interface AiModelAdapter {

    String chat(String message, AiModel model);

    String vision(String prompt, String base64Image, AiModel model);

    boolean supports(String provider);
}