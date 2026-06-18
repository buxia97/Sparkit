package com.sparkit.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流Key前缀 */
    String key() default "";

    /** 限制次数 */
    int limit() default 10;

    /** 时间窗口 */
    long timeout() default 60;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 提示信息 */
    String msg() default "请求过于频繁，请稍后再试";
}