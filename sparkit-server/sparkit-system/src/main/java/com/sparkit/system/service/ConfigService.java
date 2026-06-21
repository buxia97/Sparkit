package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.system.mapper.ConfigMapper;
import com.sparkit.system.model.entity.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService extends ServiceImpl<ConfigMapper, Config> {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取配置值
     */
    public String getConfigValue(String key) {
        // 先查缓存
        String cacheKey = CacheKeys.CONFIG_CACHE + key;
        String value = (String) redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            return value;
        }
        Config config = getOne(new LambdaQueryWrapper<Config>().eq(Config::getConfigKey, key));
        if (config != null) {
            redisTemplate.opsForValue().set(cacheKey, config.getConfigValue(), 1, TimeUnit.HOURS);
            return config.getConfigValue();
        }
        return null;
    }

    /**
     * 获取配置值（带默认值）
     */
    public String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取分组配置
     */
    public Map<String, String> getConfigByGroup(String group) {
        List<Config> configs = list(new LambdaQueryWrapper<Config>()
                .eq(Config::getConfigGroup, group)
                .eq(Config::getStatus, 1));
        return configs.stream().collect(Collectors.toMap(Config::getConfigKey, Config::getConfigValue));
    }

    @Transactional
    public void create(Config config) {
        if (count(new LambdaQueryWrapper<Config>().eq(Config::getConfigKey, config.getConfigKey())) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_EXISTS);
        }
        save(config);
        clearCache(config.getConfigKey());
    }

    /**
     * 批量保存配置（高性能：一次事务 + 批量更新SQL）
     */
    @Transactional
    public void batchSave(String group, Map<String, String> configs) {
        List<Config> configList = configs.entrySet().stream().map(entry -> {
            Config config = new Config();
            config.setConfigKey(entry.getKey());
            config.setConfigValue(entry.getValue());
            config.setConfigGroup(group);
            return config;
        }).collect(Collectors.toList());
        baseMapper.batchUpdateByKey(configList);
        // 清除缓存
        configs.keySet().forEach(this::clearCache);
    }

    /** 刷新配置缓存 */
    public void refreshCache() {
        List<Config> configs = list();
        for (Config config : configs) {
            redisTemplate.opsForValue().set(CacheKeys.CONFIG_CACHE + config.getConfigKey(),
                    config.getConfigValue(), 1, TimeUnit.HOURS);
        }
    }

    private void clearCache(String key) {
        redisTemplate.delete(CacheKeys.CONFIG_CACHE + key);
    }
}