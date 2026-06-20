package com.sparkit.framework.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userType;
    private String username;
    private String nickname;
    private String avatar;
    private Long deptId;
    private Long tenantId;
    private String tenantCode;
    private Set<String> permissions;
    private Set<Long> roleIds;
    private Boolean isAdmin;
}