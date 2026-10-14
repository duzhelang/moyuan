package com.moyuan.controller;

import com.moyuan.ai.StreamCallback;
import com.moyuan.annotation.SkipOperationLog;
import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.ChatStreamRequest;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.AiModelConfigService;
import com.moyuan.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Operation(summary = "AI流式问答")
    @SkipOperationLog
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatStreamRequest request) {
        List<Map<String, Object>> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "messages 不能为空");
        }

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 分钟
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        emitter.onError(e -> cancelled.set(true));

        aiService.streamChat(messages, request.getModuleCode(), request.getTemperature(), request.getMaxTokens(),
                new StreamCallback() {
                    @Override
                    public void onDelta(String delta) {
                        try {
                            emitter.send(SseEmitter.event().data(Map.of("delta", delta)));
                        } catch (IOException e) {
                            cancelled.set(true); // 客户端已断开
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        try {
                            emitter.send(SseEmitter.event().data(Map.of("error",
                                    error.getMessage() != null ? error.getMessage() : "生成失败")));
                        } catch (IOException ignored) {
                        } finally {
                            emitter.complete();
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                        } catch (IOException ignored) {
                        } finally {
                            emitter.complete();
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return cancelled.get();
                    }
                });
        return emitter;
    }

    @Operation(summary = "看图写诗")
    @PostMapping("/write-poem")
    public R<Map<String, String>> writePoemFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "model", defaultValue = "zhipu") String model,
            @RequestParam(value = "visionModel", required = false) String visionModel,
            @RequestParam(value = "moduleCode", required = false) String moduleCode) {

        String poem = aiService.writePoemFromImage(image, model, visionModel, moduleCode);
        String actualModel = aiModelConfigService.resolveModelName(model, moduleCode, visionModel);

        return R.success(Map.of(
                "poem", poem,
                "model", actualModel,
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
        String actualModel = aiModelConfigService.resolveModelName(model, moduleCode, visionModel);

        return R.success(Map.of(
                "text", text,
                "model", actualModel,
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
