package com.moyuan.controller;

import com.moyuan.ai.StreamCallback;
import com.moyuan.service.AiModelConfigService;
import com.moyuan.service.AiService;
import com.moyuan.service.TokenBlacklistService;
import com.moyuan.service.UserService;
import com.moyuan.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * {@code POST /api/ai/chat/stream} 的 SSE 事件格式测试。
 * 覆盖：增量 delta 事件、[DONE] 结束事件、error 事件。
 */
@WebMvcTest(value = AiController.class, properties = {"app.mapper-scan-enabled=false"})
@AutoConfigureMockMvc(addFilters = false)
class AiControllerStreamTest {

    private static final String REQUEST_BODY =
            "{\"moduleCode\":\"chat\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;
    @MockBean
    private AiModelConfigService aiModelConfigService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private TokenBlacklistService tokenBlacklistService;
    @MockBean
    private UserService userService;
    @MockBean
    private CacheManager cacheManager;
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void 流式接口应输出delta与DONE事件() throws Exception {
        doAnswer(inv -> {
            StreamCallback callback = inv.getArgument(4);
            callback.onDelta("明");
            callback.onDelta("月");
            callback.onComplete();
            return null;
        }).when(aiService).streamChat(any(), any(), any(), any(), any());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("{\"delta\":\"明\"}"), "缺少首个增量事件，实际: " + body);
        assertTrue(body.contains("{\"delta\":\"月\"}"), "缺少第二个增量事件，实际: " + body);
        assertTrue(body.contains("[DONE]"), "缺少结束事件，实际: " + body);
    }

    @Test
    void 流式接口出错时应输出error事件() throws Exception {
        doAnswer(inv -> {
            StreamCallback callback = inv.getArgument(4);
            callback.onError(new RuntimeException("上游失败"));
            return null;
        }).when(aiService).streamChat(any(), any(), any(), any(), any());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("\"error\""), "缺少 error 事件，实际: " + body);
    }
}
