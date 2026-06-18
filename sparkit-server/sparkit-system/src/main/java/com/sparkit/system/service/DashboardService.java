package com.sparkit.system.service;

import com.sparkit.system.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表盘服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AdminUserMapper adminUserMapper;

    public Map<String, Object> getDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", adminUserMapper.selectCount(null));
        result.put("todayNew", 0);
        result.put("totalArticles", 0);
        result.put("monthArticles", 0);
        result.put("totalFiles", 0);
        result.put("jobSuccessRate", "0%");
        result.put("totalJobs", 0);
        return result;
    }
}