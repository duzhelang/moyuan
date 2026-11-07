package com.moyuan.controller;

import com.moyuan.ai.StreamCallback;
import com.moyuan.annotation.SkipOperationLog;
import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.ChatStreamRequest;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.AiCallLogService;
import com.moyuan.service.AiGuardService;
import com.moyuan.service.AiModelConfigService;
import com.moyuan.service.AiService;
import com.moyuan.util.AiInputValidator;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = "AI接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiModelConfigService aiModelConfigService;
    private final AiGuardService aiGuardService;
    private final AiCallLogService aiCallLogService;

    /** 古籍识文维护开关；与前端首页 OCR_ENABLED 同步 */
    @Value("${ai.features.ocr-enabled:false}")
    private boolean ocrEnabled;

    @Operation(summary = "AI问答")
    @PostMapping("/chat")
    public R<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);

        String message = AiInputValidator.requireText(request.get("message"), "message",
                AiInputValidator.MAX_CHAT_CHARS);
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        String reply;
        long start = System.currentTimeMillis();
        try {
            reply = aiService.chat(message, model, moduleCode);
            if (!StringUtils.hasText(reply)) {
                throw new BusinessException(ResultCode.ERROR, "AI 未返回内容，请稍后重试");
            }
        } catch (RuntimeException e) {
            aiCallLogService.error("chat", model, moduleCode,
                    System.currentTimeMillis() - start, null, e.getMessage(), userId);
            throw e;
        }
        aiCallLogService.success("chat", model, moduleCode, System.currentTimeMillis() - start, null, userId);
        aiGuardService.consumeTokens(userId, AiInputValidator.estimateTokens(message, reply));

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
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);

        List<Map<String, Object>> messages = AiInputValidator.requireMessages(request.getMessages());
        Integer maxTokens = AiInputValidator.normalizeMaxTokens(request.getMaxTokens());
        Double temperature = AiInputValidator.normalizeTemperature(request.getTemperature());

        long inputTokens = AiInputValidator.estimateTokens(messages.stream()
                .map(message -> String.valueOf(message.get("content")))
                .toArray(String[]::new));
        aiGuardService.checkTokens(userId, inputTokens);

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 分钟
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicBoolean settled = new AtomicBoolean(false);
        AtomicBoolean logged = new AtomicBoolean(false);
        AtomicLong outputChars = new AtomicLong(0);
        // 流式接口的耗时从请求开始算，首字延迟从首个 delta 到达算
        long streamStart = System.currentTimeMillis();
        AtomicLong ttft = new AtomicLong(0);
        String streamModel = request.getModuleCode() != null && !request.getModuleCode().isEmpty()
                ? request.getModuleCode() : "default";

        // 调用日志只写一次，避免 timeout/error/complete 回调叠加重复记录
        java.util.function.BiConsumer<String, String> logStream = (status, errorMsg) -> {
            if (!logged.compareAndSet(false, true)) {
                return;
            }
            long duration = System.currentTimeMillis() - streamStart;
            Long ttftValue = ttft.get() == 0 ? null : ttft.get();
            if ("success".equals(status)) {
                aiCallLogService.success("chat_stream", streamModel, request.getModuleCode(),
                        duration, ttftValue, userId);
            } else {
                aiCallLogService.error("chat_stream", streamModel, request.getModuleCode(),
                        duration, ttftValue, errorMsg, userId);
            }
        };

        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            logStream.accept("error", "timeout");
            emitter.complete();
        });
        emitter.onError(e -> {
            cancelled.set(true);
            logStream.accept("error", e.getMessage());
        });

        aiService.streamChat(messages, request.getModuleCode(), temperature, maxTokens,
                new StreamCallback() {
                    @Override
                    public void onDelta(String delta) {
                        // 只记第一个 delta 的到达时间，即首字延迟
                        if (ttft.get() == 0) {
                            ttft.compareAndSet(0, System.currentTimeMillis() - streamStart);
                        }
                        outputChars.addAndGet(delta == null ? 0 : delta.length());
                        try {
                            emitter.send(SseEmitter.event().data(Map.of("delta", delta == null ? "" : delta)));
                        } catch (IOException e) {
                            cancelled.set(true); // 客户端已断开
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        settleUsage();
                        logStream.accept("error", error.getMessage() != null ? error.getMessage() : "生成失败");
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
                        settleUsage();
                        logStream.accept("success", null);
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

                    /** 成功/失败只结算一次，避免回调重复计数 */
                    private void settleUsage() {
                        if (settled.compareAndSet(false, true)) {
                            aiGuardService.consumeTokensQuietly(userId,
                                    inputTokens + AiInputValidator.estimateTokensByChars(outputChars.get()));
                        }
                    }
                });
        return emitter;
    }

    @Operation(summary = "看图写诗")
    @PostMapping("/write-poem")
    public R<Map<String, Object>> writePoemFromImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "model", defaultValue = "zhipu") String model,
            @RequestParam(value = "visionModel", required = false) String visionModel,
            @RequestParam(value = "moduleCode", required = false) String moduleCode) {

        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);
        aiGuardService.checkVisionCall(userId);

        long start = System.currentTimeMillis();
        Map<String, Object> checked;
        try {
            checked = aiService.writePoemFromImageChecked(image, model, visionModel, moduleCode);
            Object poem = checked.get("poem");
            if (!(poem instanceof String poemText) || !StringUtils.hasText(poemText)) {
                throw new BusinessException(ResultCode.ERROR, "AI 未返回诗词内容，请稍后重试");
            }
        } catch (RuntimeException e) {
            aiCallLogService.error("write_poem", model, moduleCode,
                    System.currentTimeMillis() - start, null, e.getMessage(), userId);
            throw e;
        }
        aiCallLogService.success("write_poem", model, moduleCode, System.currentTimeMillis() - start, null, userId);
        String poemText = String.valueOf(checked.get("poem"));
        aiGuardService.consumeVisionCall(userId);
        aiGuardService.consumeTokens(userId, AiInputValidator.estimateTokens(poemText));

        String actualModel = aiModelConfigService.resolveModelName(model, moduleCode, visionModel);

        return R.success(Map.of(
                "poem", poemText,
                "check", checked.get("check"),
                "rewritten", checked.get("rewritten"),
                "model", actualModel,
                "visionModel", visionModel != null ? visionModel : "default"
        ));
    }

    @Operation(summary = "智能分析")
    @PostMapping("/analyze")
    public R<Map<String, String>> analyzePoem(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);

        String poem = AiInputValidator.requireText(request.get("poem"), "poem",
                AiInputValidator.MAX_ANALYZE_CHARS);
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        String analysis;
        long start = System.currentTimeMillis();
        try {
            analysis = aiService.analyzePoem(poem, model, moduleCode);
            if (!StringUtils.hasText(analysis)) {
                throw new BusinessException(ResultCode.ERROR, "AI 未返回分析内容，请稍后重试");
            }
        } catch (RuntimeException e) {
            aiCallLogService.error("analyze", model, moduleCode,
                    System.currentTimeMillis() - start, null, e.getMessage(), userId);
            throw e;
        }
        aiCallLogService.success("analyze", model, moduleCode, System.currentTimeMillis() - start, null, userId);
        aiGuardService.consumeTokens(userId, AiInputValidator.estimateTokens(poem, analysis));

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

        if (!ocrEnabled) {
            throw new BusinessException(ResultCode.AI_FEATURE_DISABLED, "古籍识文功能维护中，暂不可用");
        }

        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);
        aiGuardService.checkVisionCall(userId);

        String text;
        long start = System.currentTimeMillis();
        try {
            text = aiService.ocrImage(image, model, visionModel, moduleCode);
            if (!StringUtils.hasText(text)) {
                throw new BusinessException(ResultCode.ERROR, "AI 未识别到文字，请更换图片后重试");
            }
        } catch (RuntimeException e) {
            aiCallLogService.error("ocr", model, moduleCode,
                    System.currentTimeMillis() - start, null, e.getMessage(), userId);
            throw e;
        }
        aiCallLogService.success("ocr", model, moduleCode, System.currentTimeMillis() - start, null, userId);
        aiGuardService.consumeVisionCall(userId);
        aiGuardService.consumeTokens(userId, AiInputValidator.estimateTokens(text));

        String actualModel = aiModelConfigService.resolveModelName(model, moduleCode, visionModel);

        return R.success(Map.of(
                "text", text,
                "model", actualModel,
                "visionModel", visionModel != null ? visionModel : "default"
        ));
    }

    @Operation(summary = "AI对对联")
    @PostMapping("/couplet")
    public R<Map<String, Object>> matchCouplet(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtil.getCurrentUserIdOrNull();
        aiGuardService.checkRequest(userId);

        String upperCouplet = AiInputValidator.requireText(request.get("upperCouplet"), "upperCouplet",
                AiInputValidator.MAX_COUPLET_CHARS);
        String model = request.getOrDefault("model", "zhipu");
        String moduleCode = request.get("moduleCode");

        Map<String, Object> checked;
        long start = System.currentTimeMillis();
        try {
            checked = aiService.matchCoupletChecked(upperCouplet, model, moduleCode);
            Object result = checked.get("result");
            if (!(result instanceof String resultText) || !StringUtils.hasText(resultText)) {
                throw new BusinessException(ResultCode.ERROR, "AI 未返回下联，请稍后重试");
            }
        } catch (RuntimeException e) {
            aiCallLogService.error("couplet", model, moduleCode,
                    System.currentTimeMillis() - start, null, e.getMessage(), userId);
            throw e;
        }
        aiCallLogService.success("couplet", model, moduleCode, System.currentTimeMillis() - start, null, userId);
        String resultText = String.valueOf(checked.get("result"));
        long tokens = AiInputValidator.estimateTokens(upperCouplet, resultText);
        if (Boolean.TRUE.equals(checked.get("rewritten"))) {
            tokens *= 2; // 重写会再调用一次模型
        }
        aiGuardService.consumeTokens(userId, tokens);

        return R.success(Map.of(
                "upperCouplet", upperCouplet,
                "result", resultText,
                "check", checked.get("check"),
                "rewritten", checked.get("rewritten"),
                "model", model
        ));
    }

    @Operation(summary = "平仄校验")
    @PostMapping("/tone-check")
    public R<Map<String, Object>> toneCheck(@RequestBody Map<String, String> request) {
        String text = AiInputValidator.requireText(request.get("text"), "text",
                AiInputValidator.MAX_TONE_CHARS);
        // 本地确定性引擎，不调用大模型，因此不计 token
        return R.success(aiService.checkTone(text));
    }

    @Operation(summary = "获取模块AI配置")
    @GetMapping("/config/{moduleCode}")
    public R<AiModuleConfig> getModuleConfig(@PathVariable String moduleCode) {
        AiModuleConfig config = aiModelConfigService.getModuleConfigByCode(moduleCode);
        return R.success(config);
    }
}
