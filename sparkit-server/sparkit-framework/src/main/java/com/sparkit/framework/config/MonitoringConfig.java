package com.sparkit.framework.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控运维配置 - Actuator / Prometheus / 健康检查
 */
@Configuration
@RequiredArgsConstructor
public class MonitoringConfig {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 数据库健康检查
     */
    @Bean
    public HealthIndicator dbHealthIndicator() {
        return () -> {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(3)) {
                    return Health.up().withDetail("database", "MySQL").build();
                }
                return Health.down().withDetail("database", "connection invalid").build();
            } catch (Exception e) {
                return Health.down(e).withDetail("database", "MySQL").build();
            }
        };
    }

    /**
     * Redis 健康检查
     */
    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                redisTemplate.opsForValue().get("health:check");
                return Health.up().withDetail("redis", "connected").build();
            } catch (Exception e) {
                return Health.down(e).withDetail("redis", "disconnected").build();
            }
        };
    }

    /**
     * 应用信息
     */
    @Bean
    public InfoContributor appInfoContributor() {
        return builder -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("app", "Sparkit");
            details.put("version", "1.0.0");
            details.put("java", System.getProperty("java.version"));
            details.put("os", System.getProperty("os.name"));
            builder.withDetails(details);
        };
    }

    /**
     * 自定义 Metrics（Prometheus）
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    public Object customMetrics(MeterRegistry registry) {
        registry.gauge("app_start_time_seconds", Tags.empty(),
                System.currentTimeMillis() / 1000.0);
        return new Object();
    }
}