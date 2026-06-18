package com.sparkit.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * CSRF 防护过滤器
 * 生成 Token 校验请求合法性
 */
@Slf4j
public class CsrfFilter implements Filter {

    private static final String CSRF_TOKEN_ATTR = "CSRF_TOKEN";
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String method = httpReq.getMethod().toUpperCase();

        // 只拦截状态变更请求
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method)) {
            // 跳过文件上传
            String contentType = httpReq.getContentType();
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                chain.doFilter(request, response);
                return;
            }

            HttpSession session = httpReq.getSession(false);
            if (session != null) {
                String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
                String requestToken = httpReq.getHeader(CSRF_HEADER);
                if (sessionToken != null && !sessionToken.equals(requestToken)) {
                    log.warn("CSRF验证失败: {}", httpReq.getRequestURI());
                    throw new SecurityException("CSRF token 验证失败");
                }
            }
        }

        chain.doFilter(request, response);
    }

    /** 生成 CSRF Token */
    public static String generateToken(HttpSession session) {
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_ATTR, token);
        return token;
    }
}