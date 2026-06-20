package com.sparkit.framework.service;

import com.sparkit.common.constant.CacheKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录策略服务
 * 支持：单设备登录、多设备登录、登录失败锁定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginStrategyService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 默认登录策略：multi=多设备，single=单设备 */
    private static final String STRATEGY_KEY = "login:strategy:";
    /** 登录失败次数 */
    private static final String FAIL_COUNT_KEY = "login:fail:";
    /** 用户已登录的设备 token */
    private static final String USER_TOKEN_KEY = "login:user_token:";
    /** 最大失败次数 */
    private static final int MAX_FAIL_COUNT = 5;
    /** 锁定时间（分钟） */
    private static final int LOCK_MINUTES = 30;
    /** 失败计数过期时间（分钟） */
    private static final int FAIL_COUNT_TTL = 60;

    /**
     * 设置登录策略
     */
    public void setLoginStrategy(String key, String strategy) {
        redisTemplate.opsForValue().set(STRATEGY_KEY + key, strategy);
        log.info("登录策略已设置: key={}, strategy={}", key, strategy);
    }

    /**
     * 获取登录策略，默认多设备
     */
    public String getLoginStrategy(String key) {
        Object strategy = redisTemplate.opsForValue().get(STRATEGY_KEY + key);
        return strategy != null ? strategy.toString() : "multi";
    }

    /**
     * 登录前检查：是否被锁定、是否单设备登录需踢出旧会话
     */
    public void preLoginCheck(Long userId, String username) {
        // 检查是否被锁定
        if (isLocked(username)) {
            throw new RuntimeException("账号已被锁定，请" + LOCK_MINUTES + "分钟后重试");
        }
    }

    /**
     * 登录成功后处理：记录会话、踢出旧设备
     */
    public void onLoginSuccess(Long userId, String username, String token) {
        // 清除失败计数
        clearFailCount(username);

        // 单设备登录：踢出旧会话
        String strategy = getLoginStrategy("global");
        if ("single".equals(strategy)) {
            String oldToken = (String) redisTemplate.opsForValue().get(USER_TOKEN_KEY + userId);
            if (oldToken != null && !oldToken.equals(token)) {
                // 将旧 token 加入黑名单
                redisTemplate.opsForValue().set("login:blacklist:" + oldToken, "1", 24, TimeUnit.HOURS);
                log.info("单设备登录，已踢出旧会话: userId={}", userId);
            }
        }

        // 记录当前 token
        redisTemplate.opsForValue().set(USER_TOKEN_KEY + userId, token, 24, TimeUnit.HOURS);
    }

    /**
     * 登录失败处理
     */
    public void onLoginFail(String username) {
        String key = FAIL_COUNT_KEY + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, FAIL_COUNT_TTL, TimeUnit.MINUTES);

        String count = (String) redisTemplate.opsForValue().get(key);
        int failCount = count != null ? Integer.parseInt(count) : 0;
        log.warn("登录失败: username={}, failCount={}", username, failCount);

        if (failCount >= MAX_FAIL_COUNT) {
            lockAccount(username);
            throw new RuntimeException("账号已被锁定，请" + LOCK_MINUTES + "分钟后重试");
        }
    }

    /**
     * 检查 token 是否在黑名单中（单设备登录被踢出）
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("login:blacklist:" + token));
    }

    /**
     * 是否被锁定
     */
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("login:lock:" + username));
    }

    /**
     * 锁定账号
     */
    public void lockAccount(String username) {
        redisTemplate.opsForValue().set("login:lock:" + username, "1", LOCK_MINUTES, TimeUnit.MINUTES);
        log.warn("账号已锁定: username={}, lockMinutes={}", username, LOCK_MINUTES);
    }

    /**
     * 解锁账号
     */
    public void unlockAccount(String username) {
        redisTemplate.delete("login:lock:" + username);
        redisTemplate.delete(FAIL_COUNT_KEY + username);
        log.info("账号已解锁: username={}", username);
    }

    /**
     * 清除失败计数
     */
    private void clearFailCount(String username) {
        redisTemplate.delete(FAIL_COUNT_KEY + username);
    }
}