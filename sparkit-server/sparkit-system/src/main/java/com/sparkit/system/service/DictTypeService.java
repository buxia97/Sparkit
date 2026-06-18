package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.system.mapper.DictTypeMapper;
import com.sparkit.system.model.entity.DictType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 字典类型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictTypeService extends ServiceImpl<DictTypeMapper, DictType> {

    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void update(DictType dictType) {
        DictType exist = getById(dictType.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        updateById(dictType);
        clearCache(dictType.getDictType());
    }

    @Transactional
    public void delete(Long id) {
        DictType dictType = getById(id);
        if (dictType != null) {
            clearCache(dictType.getDictType());
        }
        removeById(id);
    }

    private void clearCache(String dictType) {
        redisTemplate.delete(CacheKeys.DICT_CACHE + dictType);
    }

    public void refreshCache() {
        // 清除所有字典缓存
        redisTemplate.delete(CacheKeys.DICT_CACHE + "*");
    }
}