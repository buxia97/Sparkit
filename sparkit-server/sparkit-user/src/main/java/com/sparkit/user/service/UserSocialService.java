package com.sparkit.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.user.mapper.UserSocialMapper;
import com.sparkit.user.model.entity.UserSocial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户社交登录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSocialService extends ServiceImpl<UserSocialMapper, UserSocial> {

    /** 获取用户的社交绑定列表 */
    public List<UserSocial> listByUserId(Long userId) {
        return list(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getUserId, userId)
                .orderByDesc(UserSocial::getBindTime));
    }

    /** 绑定社交账号 */
    public UserSocial bind(Long userId, String platform, String openid, String unionid,
                            String nickname, String avatar, String accessToken, String refreshToken) {
        // 检查是否已绑定该平台
        UserSocial existing = getOne(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getUserId, userId)
                .eq(UserSocial::getPlatform, platform));
        if (existing != null) {
            existing.setOpenid(openid);
            existing.setUnionid(unionid);
            existing.setNickname(nickname);
            existing.setAvatar(avatar);
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            updateById(existing);
            return existing;
        }

        UserSocial social = new UserSocial();
        social.setUserId(userId);
        social.setPlatform(platform);
        social.setOpenid(openid);
        social.setUnionid(unionid);
        social.setNickname(nickname);
        social.setAvatar(avatar);
        social.setAccessToken(accessToken);
        social.setRefreshToken(refreshToken);
        social.setBindTime(java.time.LocalDateTime.now());
        save(social);

        log.info("社交账号绑定成功: userId={}, platform={}", userId, platform);
        return social;
    }

    /** 解绑社交账号 */
    public void unbind(Long userId, String platform) {
        remove(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getUserId, userId)
                .eq(UserSocial::getPlatform, platform));
        log.info("社交账号解绑: userId={}, platform={}", userId, platform);
    }

    /** 通过openid查找用户 */
    public UserSocial findByOpenid(String platform, String openid) {
        return getOne(new LambdaQueryWrapper<UserSocial>()
                .eq(UserSocial::getPlatform, platform)
                .eq(UserSocial::getOpenid, openid));
    }
}