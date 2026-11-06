package com.moyuan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「诗宝阁」服务端会话存储。
 * <p>
 * 按「用户 + 角色」隔离，Redis 保存最近 40 条消息，TTL 7 天；Redis 不可用时降级为本机内存，
 * 前端仍保留 localStorage 兜底。这里只存 user/assistant 消息，system 提示词由前端每次请求携带。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatSessionService {

    private static final String KEY_PREFIX = "ai:chat:session:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_MESSAGES = 40;
    private static final int MAX_CONTENT_CHARS = 4000;
    private static final int MAX_ID_CHARS = 64;
    private static final int MAX_SKILL_ID_CHARS = 32;
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Redis 不可用时的本机兜底 */
    private final Map<String, MemorySession> memorySessions = new ConcurrentHashMap<>();

    public List<Map<String, Object>> load(Long userId, String roleId) {
        String key = key(userId, roleId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isEmpty()) {
                return sanitize(objectMapper.readValue(json, LIST_TYPE));
            }
        } catch (Exception e) {
            log.warn("读取 AI 会话失败，使用内存兜底：{}", e.getMessage());
        }
        MemorySession session = memorySessions.get(key);
        if (session != null && session.expireAt > System.currentTimeMillis()) {
            return sanitize(session.messages);
        }
        if (session != null) {
            memorySessions.remove(key);
        }
        return List.of();
    }

    public void save(Long userId, String roleId, List<?> rawMessages) {
        List<Map<String, Object>> messages = sanitize(rawMessages);
        String key = key(userId, roleId);
        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, TTL);
            return;
        } catch (Exception e) {
            log.warn("写入 AI 会话失败，使用内存兜底：{}", e.getMessage());
        }
        memorySessions.put(key, new MemorySession(messages, System.currentTimeMillis() + TTL.toMillis()));
    }

    public void clear(Long userId, String roleId) {
        String key = key(userId, roleId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除 AI 会话失败：{}", e.getMessage());
        }
        memorySessions.remove(key);
    }

    private String key(Long userId, String roleId) {
        return KEY_PREFIX + userId + ":" + roleId;
    }

    /**
     * 只保留前端约定的字段，截断超长内容，丢弃 system 等非法角色，最多保留最近 40 条。
     */
    private List<Map<String, Object>> sanitize(List<?> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, rawMessages.size() - MAX_MESSAGES);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = start; i < rawMessages.size(); i++) {
            Object item = rawMessages.get(i);
            if (!(item instanceof Map<?, ?> message)) {
                continue;
            }
            String role = asString(message.get("role"));
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String content = asString(message.get("content"));
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > MAX_CONTENT_CHARS) {
                content = content.substring(0, MAX_CONTENT_CHARS);
            }

            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("id", truncate(asString(message.get("id")), MAX_ID_CHARS, "srv-" + i));
            normalized.put("role", role);
            normalized.put("content", content);
            String skillId = asString(message.get("skillId"));
            if (!skillId.isEmpty()) {
                normalized.put("skillId", truncate(skillId, MAX_SKILL_ID_CHARS, ""));
            }
            Object time = message.get("time");
            normalized.put("time", time instanceof Number number ? number.longValue() : System.currentTimeMillis());
            result.add(normalized);
        }
        return result;
    }

    private String asString(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private String truncate(String value, int maxChars, String fallback) {
        if (value.isEmpty()) {
            return fallback;
        }
        return value.length() > maxChars ? value.substring(0, maxChars) : value;
    }

    private record MemorySession(List<Map<String, Object>> messages, long expireAt) {
    }
}
