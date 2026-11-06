package com.moyuan.service;

import com.moyuan.common.ResultCode;
import com.moyuan.config.AiGuardProperties;
import com.moyuan.entity.User;
import com.moyuan.exception.BusinessException;
import com.moyuan.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对外接口的轻量调用保护：每分钟频率、每日请求数、每日估算 token 配额。
 * <p>
 * 计数优先写 Redis；Redis 不可用时自动降级为本机内存计数，保证功能不中断。
 * 管理员账号豁免，避免后台调试/演示被额度锁死。
 * <p>
 * 注意：只在 {@code /api/ai/**} 控制器层调用，不影响游戏、韵脚、RAG 等内部 AI 调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuardService {

    private static final String MINUTE_KEY_PREFIX = "ai:guard:min:";
    private static final String DAILY_REQUEST_KEY_PREFIX = "ai:guard:req:";
    private static final String DAILY_TOKEN_KEY_PREFIX = "ai:guard:tok:";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final long MINUTE_TTL_SECONDS = 70L;
    private static final long DAY_TTL_SECONDS = 26 * 60 * 60L;
    private static final int MEMORY_CLEANUP_THRESHOLD = 10_000;

    private final AiGuardProperties properties;
    private final StringRedisTemplate redisTemplate;

    /** key -> [窗口序号, 计数]；Redis 不可用时的本机兜底 */
    private final Map<String, long[]> memoryCounters = new ConcurrentHashMap<>();

    /** 请求入口：先校验每分钟频率，再累计每日请求数 */
    public void checkRequest(Long userId) {
        if (!needGuard(userId)) {
            return;
        }
        long now = System.currentTimeMillis();
        long minuteWindow = now / 60_000L;

        long minuteCount = increment(MINUTE_KEY_PREFIX + userId + ":" + minuteWindow,
                minuteWindow, MINUTE_TTL_SECONDS, 1);
        if (minuteCount > properties.getPerMinute()) {
            throw new BusinessException(ResultCode.AI_RATE_LIMITED,
                    "AI 请求过于频繁，请稍后再试（每分钟最多 " + properties.getPerMinute() + " 次）");
        }

        long dayWindow = LocalDate.now(ZONE).toEpochDay();
        long dailyCount = increment(DAILY_REQUEST_KEY_PREFIX + userId + ":" + dayWindow,
                dayWindow, DAY_TTL_SECONDS, 1);
        if (dailyCount > properties.getDailyRequests()) {
            throw new BusinessException(ResultCode.AI_RATE_LIMITED,
                    "今日 AI 请求次数已达上限（" + properties.getDailyRequests() + " 次），请明日再试");
        }
    }

    /** 调用前预检查 token 余额；估算值只用于防止单次超大请求 */
    public void checkTokens(Long userId, long estimatedTokens) {
        if (!needGuard(userId) || estimatedTokens <= 0) {
            return;
        }
        long used = read(DAILY_TOKEN_KEY_PREFIX + userId + ":" + dayWindow(), dayWindow());
        if (used + estimatedTokens > properties.getDailyTokens()) {
            throw new BusinessException(ResultCode.AI_QUOTA_EXCEEDED,
                    "今日 AI 使用额度已用完（约 " + properties.getDailyTokens() + " token），请明日再试");
        }
    }

    /** 调用后累计实际消耗（估算） */
    public void consumeTokens(Long userId, long tokens) {
        if (!needGuard(userId) || tokens <= 0) {
            return;
        }
        long dayWindow = dayWindow();
        increment(DAILY_TOKEN_KEY_PREFIX + userId + ":" + dayWindow,
                dayWindow, DAY_TTL_SECONDS, tokens);
    }

    /** 视觉调用前预检查：按固定 token 成本预留额度 */
    public void checkVisionCall(Long userId) {
        checkTokens(userId, properties.getVisionTokenCost());
    }

    /** 视觉调用成功后按固定 token 成本计费 */
    public void consumeVisionCall(Long userId) {
        consumeTokens(userId, properties.getVisionTokenCost());
    }

    /** 流式回调等异步场景使用：计数失败不应影响已经开始的响应 */
    public void consumeTokensQuietly(Long userId, long tokens) {
        try {
            consumeTokens(userId, tokens);
        } catch (Exception e) {
            log.warn("AI token 计数失败（已忽略）：userId={}, tokens={}, error={}",
                    userId, tokens, e.getMessage());
        }
    }

    private boolean needGuard(Long userId) {
        if (userId == null || !properties.isEnabled()) {
            return false;
        }
        try {
            User user = SecurityUtil.getCurrentUserOrNull();
            return user == null || !"admin".equalsIgnoreCase(user.getRole());
        } catch (Exception e) {
            return true;
        }
    }

    private long dayWindow() {
        return LocalDate.now(ZONE).toEpochDay();
    }

    private long increment(String key, long windowId, long ttlSeconds, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            if (value != null) {
                redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
                return value;
            }
        } catch (Exception e) {
            log.warn("Redis 计数不可用，降级为内存计数：{}", e.getMessage());
        }
        return incrementMemory(key, windowId, delta);
    }

    private long read(String key, long windowId) {
        long memoryValue = readMemory(key, windowId);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && !value.isEmpty()) {
                return Math.max(Long.parseLong(value), memoryValue);
            }
        } catch (Exception e) {
            log.warn("Redis 计数读取失败，使用内存计数：{}", e.getMessage());
        }
        return memoryValue;
    }

    private long incrementMemory(String key, long windowId, long delta) {
        cleanupMemoryIfNeeded();
        long[] value = memoryCounters.compute(key, (k, old) -> {
            if (old == null || old[0] != windowId) {
                return new long[]{windowId, delta};
            }
            old[1] += delta;
            return old;
        });
        return value[1];
    }

    private long readMemory(String key, long windowId) {
        long[] value = memoryCounters.get(key);
        return value != null && value[0] == windowId ? value[1] : 0L;
    }

    private void cleanupMemoryIfNeeded() {
        if (memoryCounters.size() < MEMORY_CLEANUP_THRESHOLD) {
            return;
        }
        long today = dayWindow();
        long minute = System.currentTimeMillis() / 60_000L;
        memoryCounters.entrySet().removeIf(entry -> {
            long window = entry.getValue()[0];
            return window != today && window != minute;
        });
    }
}
