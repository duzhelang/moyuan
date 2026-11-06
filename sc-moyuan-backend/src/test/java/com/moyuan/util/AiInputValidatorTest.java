package com.moyuan.util;

import com.moyuan.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiInputValidatorTest {

    @Test
    void 必填文本应去空白并校验长度() {
        assertEquals("明月几时有", AiInputValidator.requireText("  明月几时有  ", "poem", 10));
        assertThrows(BusinessException.class,
                () -> AiInputValidator.requireText("   ", "poem", 10));
        assertThrows(BusinessException.class,
                () -> AiInputValidator.requireText("一二三四五六", "poem", 5));
    }

    @Test
    void 消息应归一化并只保留角色与内容() {
        List<Map<String, Object>> raw = new ArrayList<>();
        raw.add(Map.of("role", "system", "content", "角色设定", "extra", "丢弃"));
        raw.add(Map.of("role", "user", "content", "  你好  "));

        List<Map<String, Object>> result = AiInputValidator.requireMessages(raw);

        assertEquals(2, result.size());
        assertEquals("user", result.get(1).get("role"));
        assertEquals("你好", result.get(1).get("content"));
        assertEquals(2, result.get(1).size(), "多余字段应被丢弃");
    }

    @Test
    void 消息应拒绝非法角色与重复system() {
        assertThrows(BusinessException.class, () -> AiInputValidator.requireMessages(List.of(
                Map.of("role", "tool", "content", "x")
        )));

        assertThrows(BusinessException.class, () -> AiInputValidator.requireMessages(List.of(
                Map.of("role", "system", "content", "a"),
                Map.of("role", "system", "content", "b"),
                Map.of("role", "user", "content", "c")
        )));
    }

    @Test
    void 消息应要求至少一条user且限制总长度() {
        assertThrows(BusinessException.class, () -> AiInputValidator.requireMessages(List.of(
                Map.of("role", "system", "content", "only system")
        )));

        List<Map<String, Object>> tooMany = new ArrayList<>();
        for (int i = 0; i <= AiInputValidator.MAX_STREAM_MESSAGES; i++) {
            tooMany.add(Map.of("role", "user", "content", "x"));
        }
        assertThrows(BusinessException.class, () -> AiInputValidator.requireMessages(tooMany));
    }

    @Test
    void 温度与最大token应限制范围() {
        assertEquals(0.7, AiInputValidator.normalizeTemperature(0.7));
        assertThrows(BusinessException.class, () -> AiInputValidator.normalizeTemperature(2.1));
        assertEquals(1024, AiInputValidator.normalizeMaxTokens(1024));
        assertThrows(BusinessException.class,
                () -> AiInputValidator.normalizeMaxTokens(AiInputValidator.MAX_STREAM_TOKENS + 1));
    }

    @Test
    void token估算应随文本增长() {
        long shortText = AiInputValidator.estimateTokens("床前明月光");
        long longText = AiInputValidator.estimateTokens("床前明月光，疑是地上霜。举头望明月，低头思故乡。");
        assertTrue(shortText > 0);
        assertTrue(longText > shortText);
    }

    @Test
    void 空值不应导致空指针() {
        Map<String, Object> nullRole = new HashMap<>();
        nullRole.put("role", null);
        nullRole.put("content", "x");
        assertThrows(BusinessException.class,
                () -> AiInputValidator.requireMessages(List.of(nullRole)));
    }
}
