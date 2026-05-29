package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "AI接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI问答")
    @PostMapping("/chat")
    public R<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String model = request.getOrDefault("model", "zhipu");

        String reply = aiService.chat(message, model);

        return R.success(Map.of(
                "message", message,
                "reply", reply,
                "model", model
        ));
    }

    @Operation(summary = "看图写诗")
    @PostMapping("/write-poem")
    public R<Map<String, String>> writePoemFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "model", defaultValue = "zhipu") String model) {

        String poem = aiService.writePoemFromImage(image, model);

        return R.success(Map.of(
                "poem", poem,
                "model", model
        ));
    }

    @Operation(summary = "智能分析")
    @PostMapping("/analyze")
    public R<Map<String, String>> analyzePoem(@RequestBody Map<String, String> request) {
        String poem = request.get("poem");
        String model = request.getOrDefault("model", "zhipu");

        String analysis = aiService.analyzePoem(poem, model);

        return R.success(Map.of(
                "poem", poem,
                "analysis", analysis,
                "model", model
        ));
    }
}