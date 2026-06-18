package com.sparkit.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * XSS 防护过滤器
 * 对请求参数中的 HTML/JS 标签进行转义
 */
@Slf4j
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        XssRequestWrapper(HttpServletRequest request) { super(request); }

        @Override
        public String getParameter(String name) {
            return clean(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = clean(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            return clean(super.getHeader(name));
        }

        private String clean(String value) {
            if (value == null) return null;
            return value.replace("<script", "&lt;script")
                    .replace("</script>", "&lt;/script&gt;")
                    .replace("javascript:", "")
                    .replace("onerror", "")
                    .replace("onload", "");
        }
    }
}