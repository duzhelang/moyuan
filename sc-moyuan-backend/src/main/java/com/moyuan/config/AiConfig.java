package com.moyuan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class AiConfig {

    private Zhipu zhipu = new Zhipu();
    private Deepseek deepseek = new Deepseek();
    private Kimi kimi = new Kimi();
    private Mimo mimo = new Mimo();
    private Nvidia nvidia = new Nvidia();

    @Data
    public static class Zhipu {
        private Api api = new Api();
    }

    @Data
    public static class Deepseek {
        private Api api = new Api();
    }

    @Data
    public static class Kimi {
        private Api api = new Api();
    }

    @Data
    public static class Mimo {
        private Api api = new Api();
    }

    @Data
    public static class Nvidia {
        private Api api = new Api();
    }

    @Data
    public static class Api {
        private String key;
        private String apiKey;
        private String baseUrl;
        private String visionModel;
    }
}