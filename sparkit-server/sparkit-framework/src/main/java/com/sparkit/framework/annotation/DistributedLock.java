package com.sparkit.framework.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 基于 Redisson 实现，支持 SpEL 表达式
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /** 锁的 key，支持 SpEL 表达式 */
    String key() default "";

    /** 锁前缀 */
    String prefix() default "lock:";

    /** 等待获取锁的时间，默认 3 秒 */
    long waitTime() default 3;

    /** 锁的持有时间，默认 10 秒 */
    long leaseTime() default 10;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}