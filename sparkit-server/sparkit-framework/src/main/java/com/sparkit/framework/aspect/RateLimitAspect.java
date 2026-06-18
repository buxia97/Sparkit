package com.sparkit.framework.aspect;

import com.sparkit.common.annotation.RateLimit;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.utils.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(point, rateLimit);
        int limit = rateLimit.limit();
        long timeout = rateLimit.timeout();
        TimeUnit timeUnit = rateLimit.timeUnit();

        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count == null) {
            return point.proceed();
        }

        if (count == 1) {
            redisTemplate.expire(key, timeout, timeUnit);
        }

        if (count > limit) {
            log.warn("请求被限流: key={}, count={}, limit={}", key, count, limit);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS.getCode(), rateLimit.msg());
        }

        return point.proceed();
    }

    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String key = rateLimit.key();
        if (key.isEmpty()) {
            key = signature.getDeclaringTypeName() + "." + signature.getName();
        }
        String ip = ServletUtils.getClientIp();
        return "sparkit:rate:" + key + ":" + ip;
    }
}