package com.sparkit.framework.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.sparkit.framework.security.LoginUser;
import com.sparkit.framework.security.SecurityContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 多租户 SQL 拦截器配置
 * 自动在 SQL 中注入 tenant_id 条件，实现数据隔离
 */
@Configuration
public class TenantConfig {

    /** 忽略多租户的表名 */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
            "sparkit_tenant", "sparkit_tenant_package", "sparkit_config", "sparkit_dict_type",
            "sparkit_dict_data", "sparkit_menu", "sparkit_dept", "sparkit_post", "sparkit_role",
            "sparkit_admin_user", "sparkit_admin_user_role", "sparkit_role_menu",
            "sparkit_region", "sparkit_i18n", "sparkit_login_log", "sparkit_oper_log",
            "sparkit_gen_table", "sparkit_gen_table_config", "sparkit_gen_table_column",
            "sparkit_job", "sparkit_job_log"
    ));

    @Bean
    public TenantLineInnerInterceptor tenantLineInnerInterceptor() {
        return new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                try {
                    LoginUser loginUser = SecurityContextHolder.getLoginUser();
                    if (loginUser != null && loginUser.getTenantId() != null) {
                        return new LongValue(loginUser.getTenantId());
                    }
                } catch (Exception ignored) {
                    // 非租户上下文，不过滤
                }
                return null;
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return IGNORE_TABLES.contains(tableName);
            }
        });
    }
}