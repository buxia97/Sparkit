package com.sparkit.user.strategy;

import java.util.Map;

/**
 * 社交登录 OAuth 策略接口
 * 微信/QQ/微博/GitHub/钉钉/企业微信 均实现此接口
 */
public interface SocialLoginStrategy {

    /** 平台标识 */
    String getPlatform();

    /** 获取授权 URL */
    String getAuthorizeUrl(String redirectUri, String state);

    /** 通过授权码获取 accessToken */
    Map<String, Object> getAccessToken(String code, String redirectUri);

    /** 通过 accessToken 获取用户信息 */
    Map<String, Object> getUserInfo(String accessToken, String openid);

    /** 刷新 accessToken */
    Map<String, Object> refreshAccessToken(String refreshToken);
}