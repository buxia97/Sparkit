package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.system.mapper.I18nMapper;
import com.sparkit.system.model.entity.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 国际化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class I18nService extends ServiceImpl<I18nMapper, I18n> {

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public Map<String, String> getI18nMap(String lang) {
        String cacheKey = CacheKeys.I18N_CACHE + lang;
        Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<I18n> list = list(new LambdaQueryWrapper<I18n>().eq(I18n::getLang, lang));
        Map<String, String> map = list.stream().collect(Collectors.toMap(I18n::getI18nKey, I18n::getI18nValue));
        redisTemplate.opsForValue().set(cacheKey, map, 1, TimeUnit.HOURS);
        return map;
    }

    public void refreshCache() {
        redisTemplate.delete(CacheKeys.I18N_CACHE + "*");
    }
}