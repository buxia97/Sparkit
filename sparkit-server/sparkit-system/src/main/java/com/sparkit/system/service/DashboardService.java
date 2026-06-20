package com.sparkit.system.service;

import com.sparkit.system.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘服务 - 对接真实数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdminUserMapper adminUserMapper;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getDashboard() {
        Map<String, Object> result = new HashMap<>();

        // 用户统计
        result.put("totalUsers", adminUserMapper.selectCount(null));
        result.put("todayNew", countTodayNew("sparkit_admin_user", "create_time"));

        // 文章统计
        result.put("totalArticles", countTable("sparkit_news_article"));
        result.put("monthArticles", countThisMonth("sparkit_news_article", "create_time"));

        // 文件统计
        result.put("totalFiles", countTable("sparkit_file_info"));

        // 任务统计
        result.put("totalJobs", countTable("sparkit_job"));
        result.put("jobSuccessRate", calcJobSuccessRate());

        // 订单统计
        result.put("totalOrders", countTable("sparkit_payment_order"));
        result.put("todayOrderAmount", todayOrderAmount());

        return result;
    }

    private long countTodayNew(String table, String timeColumn) {
        try {
            String sql = String.format("SELECT COUNT(*) FROM %s WHERE DATE(%s) = ?", table, timeColumn);
            Long count = jdbcTemplate.queryForObject(sql, Long.class, LocalDate.now().toString());
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("统计今日新增失败: table={}", table, e.getMessage());
            return 0;
        }
    }

    private long countTable(String table) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("统计表失败: table={}", table, e.getMessage());
            return 0;
        }
    }

    private long countThisMonth(String table, String timeColumn) {
        try {
            String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s >= ?", table, timeColumn);
            String startOfMonth = LocalDate.now().withDayOfMonth(1).toString();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, startOfMonth);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("统计本月数据失败: table={}", table, e.getMessage());
            return 0;
        }
    }

    private String calcJobSuccessRate() {
        try {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sparkit_job_log", Long.class);
            if (total == null || total == 0) return "100%";
            Long success = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sparkit_job_log WHERE status = 1", Long.class);
            if (success == null) success = 0L;
            return String.format("%.1f%%", success.doubleValue() / total.doubleValue() * 100);
        } catch (Exception e) {
            log.debug("计算任务成功率失败: {}", e.getMessage());
            return "100%";
        }
    }

    private String todayOrderAmount() {
        try {
            String sql = "SELECT COALESCE(SUM(amount), 0) FROM sparkit_payment_order WHERE status = 2 AND DATE(paid_time) = ?";
            java.math.BigDecimal amount = jdbcTemplate.queryForObject(sql, java.math.BigDecimal.class, LocalDate.now().toString());
            return amount != null ? amount.toPlainString() : "0";
        } catch (Exception e) {
            log.debug("统计今日订单金额失败: {}", e.getMessage());
            return "0";
        }
    }
}