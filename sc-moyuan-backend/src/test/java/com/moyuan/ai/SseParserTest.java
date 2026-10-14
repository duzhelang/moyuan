package com.moyuan.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link SseParser} 逐行解析规则单元测试。
 * 覆盖：空行/注释/非 data 行跳过、[DONE] 结束信号、正常增量、role 块、非法 JSON。
 */
class SseParserTest {

    @Test
    void 空行与注释行应跳过() {
        assertNull(SseParser.extractDelta(null));
        assertNull(SseParser.extractDelta(""));
        assertNull(SseParser.extractDelta("   "));
        assertNull(SseParser.extractDelta(": keep-alive"));
        assertNull(SseParser.extractDelta("event: message"));
        assertNull(SseParser.extractDelta("id: 1"));
        assertNull(SseParser.extractDelta("retry: 1000"));
    }

    @Test
    void DONE应返回空串作为结束信号() {
        assertEquals("", SseParser.extractDelta("data: [DONE]"));
        assertEquals("", SseParser.extractDelta("data:[DONE]"));
    }

    @Test
    void 正常增量应返回内容() {
        assertEquals("明", SseParser.extractDelta("data: {\"choices\":[{\"delta\":{\"content\":\"明\"}}]}"));
    }

    @Test
    void 首个role块与空内容应跳过() {
        assertNull(SseParser.extractDelta("data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}"));
        assertNull(SseParser.extractDelta("data: {\"choices\":[{\"delta\":{\"content\":\"\"}}]}"));
        assertNull(SseParser.extractDelta("data: {\"choices\":[{\"delta\":{\"content\":null}}]}"));
    }

    @Test
    void 非法JSON应跳过且不抛异常() {
        assertNull(SseParser.extractDelta("data: not-a-json"));
        assertNull(SseParser.extractDelta("data: {\"id\":\"x\"}"));
    }
}
