package com.sparkit.common.constant;

/**
 * Redis 缓存 Key 常量
 */
public interface CacheKeys {

    /** 字典缓存前缀 */
    String DICT_CACHE = "sparkit:dict:";

    /** 配置缓存前缀 */
    String CONFIG_CACHE = "sparkit:config:";

    /** 地区缓存 */
    String REGION_CACHE = "sparkit:region";

    /** 国际化缓存 */
    String I18N_CACHE = "sparkit:i18n:";

    /** 权限缓存前缀 */
    String PERMS_CACHE = "sparkit:perms:";

    /** 用户Token黑名单 */
    String TOKEN_BLACKLIST = "sparkit:token:blacklist:";

    /** 验证码发送限制 */
    String VERIFY_CODE_LIMIT = "sparkit:verify:limit:";

    /** 登录失败计数 */
    String LOGIN_FAIL_COUNT = "sparkit:login:fail:";

    /** 支付幂等Key */
    String PAYMENT_IDEMPOTENT = "sparkit:payment:idempotent:";
}