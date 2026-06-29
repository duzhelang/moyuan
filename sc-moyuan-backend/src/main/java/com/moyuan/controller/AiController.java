package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.service.AiModelConfigService;
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
    private final AiModelConfigService aiModelConfigService;

    @Operation(summary = "AI问答")
    @PostMapping("/chat")
    public R<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        String reply = aiService.chat(message, model, moduleCode);

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
            @RequestParam(value = "model", defaultValue = "zhipu") String model,
            @RequestParam(value = "visionModel", required = false) String visionModel,
            @RequestParam(value = "moduleCode", required = false) String moduleCode) {

        String poem = aiService.writePoemFromImage(image, model, visionModel, moduleCode);

        return R.success(Map.of(
                "poem", poem,
                "model", model,
                "visionModel", visionModel != null ? visionModel : "default"
        ));
    }

    @Operation(summary = "智能分析")
    @PostMapping("/analyze")
    public R<Map<String, String>> analyzePoem(@RequestBody Map<String, String> request) {
        String poem = request.get("poem");
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        String analysis = aiService.analyzePoem(poem, model, moduleCode);

        return R.success(Map.of(
                "poem", poem,
                "analysis", analysis,
                "model", model
        ));
    }

    @Operation(summary = "图片OCR识别")
    @PostMapping("/ocr")
    public R<Map<String, String>> ocrImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "model", defaultValue = "zhipu") String model,
            @RequestParam(value = "visionModel", required = false) String visionModel,
            @RequestParam(value = "moduleCode", required = false) String moduleCode) {

        String text = aiService.ocrImage(image, model, visionModel, moduleCode);

        return R.success(Map.of(
                "text", text,
                "model", model,
                "visionModel", visionModel != null ? visionModel : "default"
        ));
    }

    @Operation(summary = "AI对对联")
    @PostMapping("/couplet")
    public R<Map<String, String>> matchCouplet(@RequestBody Map<String, String> request) {
        String upperCouplet = request.get("upperCouplet");
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        String result = aiService.matchCouplet(upperCouplet, model, moduleCode);

        return R.success(Map.of(
                "upperCouplet", upperCouplet,
                "result", result,
                "model", model
        ));
    }

    @Operation(summary = "获取模块AI配置")
    @GetMapping("/config/{moduleCode}")
    public R<AiModuleConfig> getModuleConfig(@PathVariable String moduleCode) {
        AiModuleConfig config = aiModelConfigService.getModuleConfigByCode(moduleCode);
        return R.success(config);
    }
}
