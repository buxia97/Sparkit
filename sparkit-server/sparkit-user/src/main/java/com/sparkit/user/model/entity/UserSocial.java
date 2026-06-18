package com.sparkit.user.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户第三方账号绑定
 */
@Data
@TableName("sparkit_user_social")
public class UserSocial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String platform;
    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpireTime;
    private LocalDateTime bindTime;
}