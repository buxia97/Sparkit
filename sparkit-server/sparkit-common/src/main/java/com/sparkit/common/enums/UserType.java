package com.sparkit.common.enums;

import lombok.Getter;

/**
 * 用户类型
 */
@Getter
public enum UserType {

    ADMIN("admin", "管理员"),
    USER("user", "C端用户");

    private final String code;
    private final String desc;

    UserType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}