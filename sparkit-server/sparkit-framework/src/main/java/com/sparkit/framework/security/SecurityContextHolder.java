package com.sparkit.framework.security;

/**
 * 登录用户上下文持有者
 * 使用 ThreadLocal 存储当前请求的用户信息
 */
public class SecurityContextHolder {

    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    public static void set(LoginUser loginUser) {
        CONTEXT.set(loginUser);
    }

    public static LoginUser get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        LoginUser user = CONTEXT.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getUserType() {
        LoginUser user = CONTEXT.get();
        return user != null ? user.getUserType() : null;
    }

    public static String getUsername() {
        LoginUser user = CONTEXT.get();
        return user != null ? user.getUsername() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}