package com.sparkit.framework.security;

import com.sparkit.common.constant.Constants;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.R;
import com.sparkit.framework.config.IgnoreSecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final IgnoreSecurityConfig ignoreSecurityConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 放行白名单
        if (ignoreSecurityConfig.matches(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取 Token
        String token = extractToken(request);
        if (token == null) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            // 解析 Token
            if (!jwtTokenService.validateToken(token)) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            // 构建 LoginUser
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(jwtTokenService.getUserId(token));
            loginUser.setUserType(jwtTokenService.getUserType(token));
            loginUser.setUsername(jwtTokenService.getUsername(token));

            SecurityContextHolder.set(loginUser);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage());
            writeError(response, ErrorCode.UNAUTHORIZED);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader(Constants.ACCESS_TOKEN_HEADER);
        if (bearer != null && bearer.startsWith(Constants.TOKEN_PREFIX)) {
            return bearer.substring(Constants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        R<?> r = R.fail(errorCode.getCode(), errorCode.getMsg());
        response.getWriter().write(com.alibaba.fastjson2.JSON.toJSONString(r));
    }
}