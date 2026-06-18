package com.sparkit.common.constant;

/**
 * 系统常量
 */
public interface Constants {

    /** 逻辑删除：未删除 */
    int NOT_DELETED = 0;
    /** 逻辑删除：已删除 */
    int DELETED = 1;

    /** 菜单类型 */
    String MENU_TYPE_DIR = "D";
    String MENU_TYPE_MENU = "M";
    String MENU_TYPE_BUTTON = "B";

    /** 数据权限范围 */
    int DATA_SCOPE_ALL = 1;
    int DATA_SCOPE_CUSTOM = 2;
    int DATA_SCOPE_DEPT = 3;
    int DATA_SCOPE_DEPT_AND_CHILD = 4;
    int DATA_SCOPE_SELF = 5;

    /** Token */
    String TOKEN_PREFIX = "Bearer ";
    String ACCESS_TOKEN_HEADER = "Authorization";
    String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    String REQUEST_ID_HEADER = "X-Request-Id";

    /** 验证码 */
    String CAPTCHA_KEY = "captcha:";
    long CAPTCHA_EXPIRE = 300;

    /** 登录失败锁定 */
    int LOGIN_MAX_RETRY = 5;
    long LOGIN_LOCK_MINUTES = 30;

    /** 验证码发送间隔（秒） */
    long VERIFY_CODE_INTERVAL = 60;
    /** 验证码有效期（秒） */
    long VERIFY_CODE_EXPIRE = 300;
    /** 同一IP每小时发送上限 */
    int VERIFY_CODE_IP_LIMIT = 10;

    /** 分页上限 */
    long PAGE_SIZE_MAX = 100;

    /** 支付订单过期时间（分钟） */
    int PAYMENT_EXPIRE_MINUTES = 30;
}