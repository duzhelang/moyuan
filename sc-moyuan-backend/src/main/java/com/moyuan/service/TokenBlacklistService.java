package com.moyuan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token 失效黑名单服务
 * <p>
 * 用途：登录登出后使已签发的 access token 立即失效。
 * 实现：以 token 的 jti 为主键写入 Redis（key: token:blacklist:{jti}，TTL 与 access token 有效期对齐）。
 * 在 Redis 不可用（未启动 / 连接失败）时优雅降级为内存 ConcurrentHashMap，保证功能不中断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final long DEFAULT_BLACKLIST_TTL_MS = 24 * 60 * 60 * 1000L; // 兜底 24h

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 不可用时的内存兜底存储
    private final ConcurrentHashMap<String, Long> memoryStore = new ConcurrentHashMap<>();

    /**
     * 将指定 jti 加入黑名单，黑名单有效期默认取 access token 剩余有效期
     */
    public void addToBlacklist(String jti, long ttlMillis) {
        if (jti == null || jti.isEmpty()) {
            return;
        }
        long ttl = ttlMillis > 0 ? ttlMillis : DEFAULT_BLACKLIST_TTL_MS;
        try {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, Boolean.TRUE, ttl, TimeUnit.MILLISECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 不可用，token 黑名单降级为内存存储");
            memoryStore.put(jti, System.currentTimeMillis() + ttl);
        } catch (Exception e) {
            log.warn("写入 token 黑名单失败: {}", e.getMessage());
            memoryStore.put(jti, System.currentTimeMillis() + ttl);
        }
    }

    /**
     * 判断指定 jti 是否已被拉黑
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 不可用，token 黑名单改为查询内存存储");
            return checkMemoryStore(jti);
        } catch (Exception e) {
            log.warn("查询 token 黑名单失败: {}", e.getMessage());
            return checkMemoryStore(jti);
        }
    }

    private boolean checkMemoryStore(String jti) {
        Long expireAt = memoryStore.get(jti);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            memoryStore.remove(jti);
            return false;
        }
        return true;
    }
}