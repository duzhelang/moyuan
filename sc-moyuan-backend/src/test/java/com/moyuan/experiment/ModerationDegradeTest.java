package com.moyuan.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.ai.AiModelRegistry;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.service.AiModerationService;
import com.moyuan.service.AiService;
import com.moyuan.service.ReportService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 风控降级验证（论文 9.7 引用）：模型超时/抛异常、返回不可解析文本时，
 * analyze 必须返回低风险且不建单，保证发布链路不被阻断。
 */
class ModerationDegradeTest {

    private AiModerationService service(AiService aiService) {
        AiModelRegistry registry = mock(AiModelRegistry.class);
        AiModuleConfig config = new AiModuleConfig();
        config.setModuleCode(AiModerationService.MODULE_CODE);
        when(registry.getModuleConfig(AiModerationService.MODULE_CODE)).thenReturn(config);
        ReportService reportService = mock(ReportService.class);
        return new AiModerationService(aiService, registry, reportService, new ObjectMapper());
    }

    @Test
    void modelTimeoutDegradesToLow() {
        AiService aiService = mock(AiService.class);
        when(aiService.chat(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Read timed out"));
        AiModerationService svc = service(aiService);

        long t0 = System.nanoTime();
        AiModerationService.ModerationResult r = svc.analyze("comment", null, "测试内容：加微信领课程");
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertEquals("low", r.riskLevel());
        assertFalse(r.shouldReport(), "降级后不应生成举报单");
        assertTrue(ms < 500, "降级应立即返回");
    }

    @Test
    void unparsableOutputDegradesToLow() {
        AiService aiService = mock(AiService.class);
        when(aiService.chat(anyString(), any(), anyString()))
                .thenReturn("抱歉，我无法判断该内容是否存在风险。");
        AiModerationService svc = service(aiService);

        AiModerationService.ModerationResult r = svc.analyze("post", "标题", "正文内容");
        assertEquals("low", r.riskLevel());
        assertFalse(r.shouldReport());
    }

    @Test
    void validJsonIsParsed() {
        AiService aiService = mock(AiService.class);
        when(aiService.chat(anyString(), any(), anyString()))
                .thenReturn("```json\n{\"riskLevel\":\"high\",\"categories\":[\"spam\",\"illegal\"],\"reason\":\"含联系方式与交易信息\"}\n```");
        AiModerationService svc = service(aiService);

        AiModerationService.ModerationResult r = svc.analyze("post", "标题", "正文");
        assertEquals("high", r.riskLevel());
        assertEquals(2, r.categories().size());
        assertTrue(r.shouldReport());
        assertTrue(r.isHigh());
    }

    @Test
    void unknownLevelFallsBackToLow() {
        AiService aiService = mock(AiService.class);
        when(aiService.chat(anyString(), any(), anyString()))
                .thenReturn("{\"riskLevel\":\"critical\",\"categories\":[\"other\"],\"reason\":\"x\"}");
        AiModerationService svc = service(aiService);

        AiModerationService.ModerationResult r = svc.analyze("post", null, "正文");
        assertEquals("low", r.riskLevel());
    }

    @Test
    void blankContentSkipsModel() {
        AiService aiService = mock(AiService.class);
        AiModerationService svc = service(aiService);

        AiModerationService.ModerationResult r = svc.analyze("comment", null, "   ");
        assertEquals("low", r.riskLevel());
        verify(aiService, never()).chat(anyString(), any(), anyString());
    }
}
