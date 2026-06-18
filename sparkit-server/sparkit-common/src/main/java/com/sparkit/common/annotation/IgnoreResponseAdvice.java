package com.sparkit.common.annotation;

import java.lang.annotation.*;

/**
 * 忽略统一响应包装
 * 标注此注解的 Controller 方法将返回原始结果，不被 R 包装
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreResponseAdvice {
}