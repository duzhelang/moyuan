package com.moyuan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 接口调用保护配置，绑定自 {@code ai.guard.*}。
 * <p>
 * 仅由 {@code /api/ai/**} 的对外控制器使用；游戏、韵脚、RAG 等内部调用不经过该保护。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.guard")
public class AiGuardProperties {

    /** 总开关，关闭后仅保留入参校验 */
    private boolean enabled = true;

    /** 单用户每分钟最大请求数 */
    private int perMinute = 60;

    /** 单用户每日最大请求数 */
    private long dailyRequests = 1000;

    /** 单用户每日估算 token 上限 */
    private long dailyTokens = 1_000_000L;

    /** 单次视觉调用的固定 token 计费 */
    private long visionTokenCost = 1000L;
}
