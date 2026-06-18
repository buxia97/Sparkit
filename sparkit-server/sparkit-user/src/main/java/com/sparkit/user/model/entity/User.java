package com.sparkit.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * C端用户
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_user")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private LocalDateTime birthday;
    private Integer status;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private String openid;
    private String unionid;
    private String registerSource;
    private String registerIp;
    private Integer level;
    private String realName;
    private String idCard;
    private Integer realNameStatus;
    private Integer isBlacklisted;
    @TableLogic
    private Integer deleted;
}