package com.moyuan.service;

import com.moyuan.service.impl.PingZeServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平仄服务单元测试：仅覆盖不依赖数据库的标注、校验与查询串识别逻辑。
 */
class PingZeServiceTest {

    private final PingZeService pingZeService = new PingZeServiceImpl(null, null);

    @Test
    void 标注白日依山尽平仄串() {
        Map<String, Object> result = pingZeService.checkTone("白日依山尽");
        List<Map<String, Object>> lines = linesOf(result);
        assertEquals(1, lines.size());
        // 白、日为入声字，强制判仄；依、山为平；尽为仄
        assertEquals("仄仄平平仄", lines.get(0).get("pattern"));
    }

    @Test
    void 登鹳雀楼应通过格律校验() {
        Map<String, Object> result = pingZeService.checkTone("白日依山尽，黄河入海流。欲穷千里目，更上一层楼。");
        assertEquals("五言绝句", result.get("genre"));
        assertEquals(Boolean.TRUE, result.get("passed"), "格律问题：" + result.get("issues"));
    }

    @Test
    void 偶数句不押韵应报问题() {
        // 第 4 句以「山」收尾，与第 2 句「流」不同韵部
        Map<String, Object> result = pingZeService.checkTone("白日依山尽，黄河入海流。欲穷千里目，更上青山。");
        assertEquals(Boolean.FALSE, result.get("passed"));
        String issues = String.valueOf(result.get("issues"));
        assertTrue(issues.contains("押韵") || issues.contains("平声"), issues);
    }

    @Test
    void 三平尾应报问题() {
        // 「桃花开」三字皆平，句末构成三平尾
        Map<String, Object> result = pingZeService.checkTone("桃花开");
        String issues = String.valueOf(result.get("issues"));
        assertTrue(issues.contains("三平尾"), issues);
    }

    @Test
    void 仅平仄中构成的查询串才识别为格律查询() {
        assertTrue(pingZeService.isPingZeQuery("平平仄仄平平仄"));
        assertTrue(pingZeService.isPingZeQuery("平平 中仄"));
        assertFalse(pingZeService.isPingZeQuery("李白"));
        assertFalse(pingZeService.isPingZeQuery("平"));
        assertFalse(pingZeService.isPingZeQuery("平平仄仄abc"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> linesOf(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("lines");
    }
}
