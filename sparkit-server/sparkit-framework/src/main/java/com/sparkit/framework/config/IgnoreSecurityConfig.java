package com.sparkit.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全白名单配置
 */
@Component
@ConfigurationProperties(prefix = "sparkit.security")
public class IgnoreSecurityConfig {

    private List<String> ignoreUrls;

    public List<String> getIgnoreUrls() {
        return ignoreUrls;
    }

    public void setIgnoreUrls(List<String> ignoreUrls) {
        this.ignoreUrls = ignoreUrls;
    }

    public boolean matches(String uri) {
        if (ignoreUrls == null) return false;
        for (String pattern : ignoreUrls) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) return true;
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }
}