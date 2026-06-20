package com.sparkit.framework.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.framework.mapper.IpBlacklistMapper;
import com.sparkit.framework.model.entity.IpBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * IP 黑名单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlacklistService extends ServiceImpl<IpBlacklistMapper, IpBlacklist> {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String IP_BLACKLIST_KEY = "ip:blacklist:";

    /**
     * 封禁 IP
     */
    public IpBlacklist banIp(String ip, String reason, Integer duration) {
        // 检查是否已封禁
        IpBlacklist existing = lambdaQuery().eq(IpBlacklist::getIp, ip).eq(IpBlacklist::getStatus, 1).one();
        if (existing != null) {
            return existing;
        }

        IpBlacklist blacklist = new IpBlacklist();
        blacklist.setIp(ip);
        blacklist.setReason(reason);
        blacklist.setDuration(duration);
        blacklist.setStatus(1);
        if (duration != null && duration > 0) {
            blacklist.setExpireTime(LocalDateTime.now().plusMinutes(duration));
        }
        save(blacklist);

        // 缓存到 Redis
        cacheBanIp(ip, duration);

        log.info("IP 已封禁: ip={}, reason={}, duration={}", ip, reason, duration);
        return blacklist;
    }

    /**
     * 解封 IP
     */
    public void unbanIp(String ip) {
        lambdaUpdate().eq(IpBlacklist::getIp, ip).set(IpBlacklist::getStatus, 0).update();
        redisTemplate.delete(IP_BLACKLIST_KEY + ip);
        log.info("IP 已解封: ip={}", ip);
    }

    /**
     * 检查 IP 是否被封禁
     */
    public boolean isBanned(String ip) {
        // 先查 Redis 缓存
        if (Boolean.TRUE.equals(redisTemplate.hasKey(IP_BLACKLIST_KEY + ip))) {
            return true;
        }

        // 再查数据库
        IpBlacklist blacklist = lambdaQuery()
                .eq(IpBlacklist::getIp, ip)
                .eq(IpBlacklist::getStatus, 1)
                .one();
        if (blacklist != null) {
            // 检查是否过期
            if (blacklist.getExpireTime() != null && blacklist.getExpireTime().isBefore(LocalDateTime.now())) {
                unbanIp(ip);
                return false;
            }
            cacheBanIp(ip, 1440); // 缓存 24 小时
            return true;
        }
        return false;
    }

    private void cacheBanIp(String ip, Integer duration) {
        int ttl = (duration != null && duration > 0) ? duration : 1440;
        redisTemplate.opsForValue().set(IP_BLACKLIST_KEY + ip, "1", ttl, TimeUnit.MINUTES);
    }

    /**
     * 获取所有启用中的黑名单 IP
     */
    public Set<String> getBannedIps() {
        return redisTemplate.keys(IP_BLACKLIST_KEY + "*");
    }
}