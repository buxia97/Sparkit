package com.sparkit.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 数据脱敏工具类
 */
public class DesensitizeUtils {

    /** 手机号脱敏 */
    public static String phone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 11) return phone;
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /** 邮箱脱敏 */
    public static String email(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) return email;
        int idx = email.indexOf("@");
        String prefix = email.substring(0, idx);
        String suffix = email.substring(idx);
        if (prefix.length() <= 2) return "*" + suffix;
        return prefix.charAt(0) + "***" + prefix.charAt(prefix.length() - 1) + suffix;
    }

    /** 身份证脱敏 */
    public static String idCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 18) return idCard;
        return idCard.replaceAll("(\\d{3})\\d{11}(\\d{4})", "$1***********$2");
    }

    /** 通用脱敏：保留首尾字符 */
    public static String common(String value, int keepHead, int keepTail) {
        if (StrUtil.isBlank(value)) return value;
        int len = value.length();
        if (keepHead + keepTail >= len) return value;
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, keepHead);
        sb.append("*".repeat(len - keepHead - keepTail));
        sb.append(value.substring(len - keepTail));
        return sb.toString();
    }
}