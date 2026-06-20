package com.sparkit.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 在线用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String ONLINE_KEY = "online:user:";
    private static final String ONLINE_TOKEN_KEY = "online:token:";

    /**
     * 用户上线
     */
    public void online(Long userId, String username, String ip, String token) {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("userId", userId.toString());
        info.put("username", username);
        info.put("ip", ip);
        info.put("loginTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        redisTemplate.opsForValue().set(ONLINE_TOKEN_KEY + token, userId.toString(), 24, TimeUnit.HOURS);
        redisTemplate.opsForHash().putAll(ONLINE_KEY + userId, info);
        redisTemplate.expire(ONLINE_KEY + userId, 24, TimeUnit.HOURS);
        log.debug("用户上线: userId={}, username={}", userId, username);
    }

    /**
     * 用户下线
     */
    public void offline(Long userId) {
        redisTemplate.delete(ONLINE_KEY + userId);
        log.debug("用户下线: userId={}", userId);
    }

    /**
     * 强制下线
     */
    public void forceOffline(Long userId) {
        // 获取用户 token 并加入黑名单
        Set<String> tokenKeys = redisTemplate.keys(ONLINE_TOKEN_KEY + "*");
        if (tokenKeys != null) {
            for (String key : tokenKeys) {
                String val = (String) redisTemplate.opsForValue().get(key);
                if (val != null && val.equals(userId.toString())) {
                    redisTemplate.opsForValue().set("login:blacklist:" + key.replace(ONLINE_TOKEN_KEY, ""), "1", 24, TimeUnit.HOURS);
                }
            }
        }
        redisTemplate.delete(ONLINE_KEY + userId);
        log.info("强制下线: userId={}", userId);
    }

    /**
     * 获取在线用户列表
     */
    public List<Map<String, Object>> listOnlineUsers() {
        Set<String> keys = redisTemplate.keys(ONLINE_KEY + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : keys) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            Map<String, Object> user = new LinkedHashMap<>();
            entries.forEach((k, v) -> user.put(k.toString(), v));
            result.add(user);
        }
        return result;
    }

    /**
     * 在线用户数
     */
    public long countOnline() {
        Set<String> keys = redisTemplate.keys(ONLINE_KEY + "*");
        return keys != null ? keys.size() : 0;
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY + userId));
    }
}