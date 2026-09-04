package com.moyuan.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 扫描配置。
 *
 * <p>通过属性 app.mapper-scan-enabled 控制是否启用 Mapper 扫描（默认启用）。
 * 测试环境（如 @WebMvcTest 切片测试）可通过设置为 false 关闭扫描，
 * 避免在无数据源 / sqlSessionFactory 的上下文中创建 Mapper Bean 而失败。
 */
@Configuration
@ConditionalOnProperty(name = "app.mapper-scan-enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.moyuan.mapper")
public class MapperScanConfig {
}
