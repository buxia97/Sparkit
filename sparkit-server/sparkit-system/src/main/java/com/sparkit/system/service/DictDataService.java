package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.system.mapper.DictDataMapper;
import com.sparkit.system.model.entity.DictData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 字典数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictDataService extends ServiceImpl<DictDataMapper, DictData> {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据字典类型获取字典数据（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<DictData> getByDictType(String dictType) {
        String cacheKey = CacheKeys.DICT_CACHE + dictType;
        List<DictData> cached = (List<DictData>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<DictData> list = baseMapper.selectByDictType(dictType);
        redisTemplate.opsForValue().set(cacheKey, list, 1, TimeUnit.HOURS);
        return list;
    }

    @Transactional
    public void update(DictData dictData) {
        updateById(dictData);
        clearCache(dictData.getDictType());
    }

    @Transactional
    public void delete(Long id) {
        DictData dictData = getById(id);
        if (dictData != null) {
            clearCache(dictData.getDictType());
        }
        removeById(id);
    }

    private void clearCache(String dictType) {
        redisTemplate.delete(CacheKeys.DICT_CACHE + dictType);
    }
}