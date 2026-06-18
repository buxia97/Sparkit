package com.sparkit.common.annotation;

import com.sparkit.common.enums.UserType;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /** 操作模块/标题 */
    String title() default "";

    /** 操作类型 */
    String operType() default "OTHER";

    /** 用户类型 */
    UserType userType() default UserType.ADMIN;

    /** 是否记录请求参数 */
    boolean saveParams() default true;

    /** 是否记录响应结果 */
    boolean saveResult() default true;
}