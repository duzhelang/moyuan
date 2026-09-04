package com.moyuan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 各厂商密钥配置属性。
 * 绑定自 ai.providers.*（密钥统一维护在 secrets/application-secrets.yml，
 * 不落库、不入代码）。数据库 ai_model.api_key 为空或占位符时，
 * AiModelRegistry 会按 provider 用此处配置的 api-key 覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.providers")
public class AiProviderProperties {

    private ProviderItem zhipu;
    private ProviderItem deepseek;
    private ProviderItem openrouter;
    private ProviderItem nvidia;
    private ProviderItem mimo;
    private ProviderItem kimi;
    private ProviderItem qwen;

    /**
     * 按提供商名获取配置的 API 密钥
     */
    public String getApiKey(String provider) {
        ProviderItem item = get(provider);
        return item == null ? null : item.getApiKey();
    }

    private ProviderItem get(String provider) {
        if (provider == null) {
            return null;
        }
        switch (provider) {
            case "zhipu":
                return zhipu;
            case "deepseek":
                return deepseek;
            case "openrouter":
                return openrouter;
            case "nvidia":
                return nvidia;
            case "mimo":
                return mimo;
            case "kimi":
                return kimi;
            case "qwen":
                return qwen;
            default:
                return null;
        }
    }

    @Data
    public static class ProviderItem {
        private String apiKey;
    }
}