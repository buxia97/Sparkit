package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.system.mapper.RegionMapper;
import com.sparkit.system.model.entity.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 地区服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionService extends ServiceImpl<RegionMapper, Region> {

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<Region> getRegionTree() {
        List<Region> cached = (List<Region>) redisTemplate.opsForValue().get(CacheKeys.REGION_CACHE);
        if (cached != null) {
            return cached;
        }
        List<Region> regions = list();
        List<Region> tree = buildTree(regions, null);
        redisTemplate.opsForValue().set(CacheKeys.REGION_CACHE, tree, 1, TimeUnit.DAYS);
        return tree;
    }

    public List<Region> getByParentCode(String parentCode) {
        return list(new LambdaQueryWrapper<Region>().eq(Region::getParentCode, parentCode));
    }

    private List<Region> buildTree(List<Region> regions, String parentCode) {
        return regions.stream()
                .filter(r -> parentCode == null ? r.getParentCode() == null : parentCode.equals(r.getParentCode()))
                .peek(r -> r.setChildren(buildTree(regions, r.getCode())))
                .collect(Collectors.toList());
    }
}