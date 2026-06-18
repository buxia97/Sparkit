package com.sparkit.framework.handler;

import com.sparkit.common.annotation.IgnoreResponseAdvice;
import com.sparkit.common.model.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应包装
 */
@RestControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 标注了 IgnoreResponseAdvice 注解的方法不包装
        if (returnType.hasMethodAnnotation(IgnoreResponseAdvice.class)) {
            return false;
        }
        // 已经是 R 类型的不再包装
        return !returnType.getParameterType().equals(R.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return R.ok();
        }
        if (body instanceof String) {
            return com.alibaba.fastjson2.JSON.toJSONString(R.ok(body));
        }
        return R.ok(body);
    }
}