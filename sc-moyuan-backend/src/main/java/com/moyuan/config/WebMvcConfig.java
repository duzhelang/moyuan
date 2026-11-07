package com.moyuan.config;

import com.moyuan.interceptor.ApiMetricInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${spring.web.resources.cache.period:3600}")
    private int cachePeriod;

    private final ApiMetricInterceptor apiMetricInterceptor;

    public WebMvcConfig(ApiMetricInterceptor apiMetricInterceptor) {
        this.apiMetricInterceptor = apiMetricInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(cachePeriod)
                .resourceChain(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 接口指标采集：仅 /api/**，健康检查与指标查询自身不记账
        registry.addInterceptor(apiMetricInterceptor).addPathPatterns("/api/**");
    }
}
