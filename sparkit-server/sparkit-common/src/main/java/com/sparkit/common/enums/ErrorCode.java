package com.sparkit.common.enums;

import lombok.Getter;

/**
 * 业务错误码
 */
@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户模块 10001+
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "用户名或密码错误"),
    USER_LOCKED(10003, "账号已被锁定，请{0}分钟后重试"),
    USER_DISABLED(10004, "账号已被禁用"),
    USER_EXISTS(10005, "用户名已存在"),
    VERIFY_CODE_ERROR(10006, "验证码错误"),
    VERIFY_CODE_EXPIRED(10007, "验证码已过期"),
    VERIFY_CODE_SEND_LIMIT(10008, "验证码发送过于频繁，请{0}秒后重试"),
    PHONE_EXISTS(10009, "手机号已注册"),
    EMAIL_EXISTS(10010, "邮箱已注册"),
    OLD_PASSWORD_ERROR(10011, "原密码错误"),
    USER_BLACKLIST(10012, "该账号已被限制访问"),
    SOCIAL_LOGIN_FAIL(10013, "社交登录失败: {0}"),

    // 权限模块 20001+
    ROLE_EXISTS(20001, "角色名称已存在"),
    ROLE_KEY_EXISTS(20002, "角色标识已存在"),
    ROLE_BUILT_IN(20003, "内置角色不可删除"),
    MENU_HAS_CHILDREN(20004, "存在子菜单，无法删除"),
    MENU_HAS_ROLE(20005, "菜单已被角色使用，无法删除"),
    DEPT_HAS_CHILDREN(20006, "存在子部门，无法删除"),
    DEPT_HAS_USER(20007, "部门下存在用户，无法删除"),

    // 配置模块 30001+
    CONFIG_KEY_EXISTS(30001, "配置键已存在"),

    // 文件模块 40001+
    FILE_NOT_FOUND(40001, "文件不存在"),
    FILE_UPLOAD_FAIL(40002, "文件上传失败"),
    FILE_CHUNK_INVALID(40003, "分片信息无效"),
    FILE_MD5_MISMATCH(40004, "文件MD5校验失败"),

    // 支付模块 50001+
    PAYMENT_FAIL(50001, "支付失败"),
    PAYMENT_ORDER_NOT_FOUND(50002, "支付订单不存在"),
    PAYMENT_REFUND_EXCEED(50003, "退款金额超过订单金额"),
    PAYMENT_IDEMPOTENT(50004, "请勿重复提交"),
    PAYMENT_SIGN_ERROR(50005, "签名验证失败"),
    ORDER_NOT_FOUND(50006, "订单不存在"),
    ORDER_STATUS_ERROR(50007, "订单状态不允许退款"),
    REFUND_AMOUNT_EXCEED(50008, "退款金额超过可退金额"),

    // 通知模块 60001+
    NOTIFY_TEMPLATE_NOT_FOUND(60001, "通知模板不存在"),
    NOTIFY_SEND_FAIL(60002, "通知发送失败"),

    // AI 模块 70001+
    AI_CONFIG_NOT_FOUND(70001, "AI模型配置不存在"),
    AI_SESSION_NOT_FOUND(70002, "AI会话不存在"),
    AI_GENERATE_FAIL(70003, "AI生成失败"),

    // 定时任务 80001+
    JOB_EXISTS(80001, "任务已存在"),
    JOB_NOT_FOUND(80002, "任务不存在"),

    // 租户 90001+
    TENANT_EXPIRED(90001, "租户已过期"),
    TENANT_DISABLED(90002, "租户已停用"),

    ;

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}