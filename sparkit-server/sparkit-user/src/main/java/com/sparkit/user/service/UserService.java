package com.sparkit.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.CacheKeys;
import com.sparkit.common.constant.Constants;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.framework.security.JwtTokenService;
import com.sparkit.framework.security.LoginUser;
import com.sparkit.user.mapper.UserMapper;
import com.sparkit.user.model.entity.User;
import com.sparkit.user.strategy.SocialLoginStrategy;
import com.sparkit.user.strategy.SocialLoginStrategyFactory;
import com.sparkit.user.strategy.QQLoginStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务（C端）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final EmailService emailService;
    private final SocialLoginStrategyFactory socialLoginStrategyFactory;

    /**
     * 手机号注册
     */
    @Transactional
    public Map<String, Object> registerByPhone(String phone, String password, String code) {
        // 校验验证码
        verifyCode(phone, code);

        // 检查手机号是否已注册
        if (count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        User user = new User();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setStatus(1);
        user.setRegisterSource("phone");
        save(user);

        return buildLoginResult(user);
    }

    /**
     * 邮箱注册
     */
    @Transactional
    public Map<String, Object> registerByEmail(String email, String password, String code) {
        verifyCode(email, code);

        if (count(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(email.substring(0, email.indexOf("@")));
        user.setStatus(1);
        user.setRegisterSource("email");
        save(user);

        return buildLoginResult(user);
    }

    /**
     * 手机号验证码登录
     */
    public Map<String, Object> loginByPhone(String phone, String code) {
        verifyCode(phone, code);

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        checkUserStatus(user);

        updateLoginInfo(user);
        return buildLoginResult(user);
    }

    /**
     * 账号密码登录
     */
    public Map<String, Object> loginByPassword(String account, String password) {
        // 检查登录失败
        String failKey = CacheKeys.LOGIN_FAIL_COUNT + "user:" + account;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount >= Constants.LOGIN_MAX_RETRY) {
            throw new BusinessException(ErrorCode.USER_LOCKED, Constants.LOGIN_LOCK_MINUTES);
        }

        User user = getOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, account).or().eq(User::getPhone, account).or().eq(User::getEmail, account)));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        checkUserStatus(user);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            Long increment = redisTemplate.opsForValue().increment(failKey, 1);
            if (increment != null && increment == 1) {
                redisTemplate.expire(failKey, Constants.LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        redisTemplate.delete(failKey);
        updateLoginInfo(user);
        return buildLoginResult(user);
    }

    /**
     * 发送验证码
     */
    public void sendVerifyCode(String target, String type) {
        // 检查发送频率
        String limitKey = CacheKeys.VERIFY_CODE_LIMIT + target;
        if (redisTemplate.hasKey(limitKey)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_SEND_LIMIT, Constants.VERIFY_CODE_INTERVAL);
        }

        // 生成验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 存储验证码
        String codeKey = Constants.CAPTCHA_KEY + target;
        redisTemplate.opsForValue().set(codeKey, code, Constants.VERIFY_CODE_EXPIRE, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(limitKey, "1", Constants.VERIFY_CODE_INTERVAL, TimeUnit.SECONDS);

        // 实际发送
        boolean sent = false;
        if ("phone".equals(type)) {
            sent = smsService.sendVerifyCode(target, code);
        } else if ("email".equals(type)) {
            sent = emailService.sendVerifyCode(target, code);
        }
        if (!sent) {
            log.warn("验证码发送失败，但仍存储到 Redis: target={}, type={}, code={}", target, type, code);
        }
    }

    private void verifyCode(String target, String code) {
        String codeKey = Constants.CAPTCHA_KEY + target;
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_EXPIRED);
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }
        redisTemplate.delete(codeKey);
    }

    private void checkUserStatus(User user) {
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
    }

    private void updateLoginInfo(User user) {
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
    }

    private Map<String, Object> buildLoginResult(User user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUserType("user");
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setAvatar(user.getAvatar());

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", jwtTokenService.generateAccessToken(user.getId(), "user", user.getUsername()));
        result.put("refreshToken", jwtTokenService.generateRefreshToken(user.getId(), "user", user.getUsername()));
        result.put("userInfo", loginUser);
        return result;
    }

    /**
     * 管理端分页查询用户
     */
    public PageResult<User> adminPage(PageQuery query, String keyword, String phone, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.isNotBlank(keyword), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .like(StringUtils.isNotBlank(phone), User::getPhone, phone)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getCreateTime);
        Page<User> page = page(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return PageResult.of(page);
    }

    /**
     * 社交登录（微信/QQ/微博/GitHub/钉钉/企业微信等）
     * 通过授权码完整 OAuth 流程
     */
    @Transactional
    public Map<String, Object> loginBySocial(String platform, String code, String redirectUri) {
        SocialLoginStrategy strategy = socialLoginStrategyFactory.getStrategy(platform);

        // 1. 通过 code 获取 access_token
        Map<String, Object> tokenResult = strategy.getAccessToken(code, redirectUri);
        if (tokenResult.containsKey("error")) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAIL, (String) tokenResult.get("error"));
        }

        String accessToken = (String) tokenResult.get("access_token");
        String refreshToken = (String) tokenResult.get("refresh_token");
        String openid = (String) tokenResult.get("openid");
        String unionid = (String) tokenResult.get("unionid");

        // QQ 需要额外获取 openid
        if ("qq".equals(platform) && openid == null) {
            QQLoginStrategy qqStrategy = (QQLoginStrategy) strategy;
            Map<String, Object> openIdResult = qqStrategy.getOpenId(accessToken);
            openid = (String) openIdResult.get("openid");
        }

        // 2. 通过 access_token 获取用户信息
        Map<String, Object> userInfo = strategy.getUserInfo(accessToken, openid);
        String nickname = null;
        String avatar = null;
        if (!userInfo.containsKey("error")) {
            nickname = (String) userInfo.getOrDefault("nickname",
                    userInfo.getOrDefault("screen_name",
                            userInfo.getOrDefault("login",
                                    userInfo.getOrDefault("name", platform + "用户"))));
            avatar = (String) userInfo.getOrDefault("avatar_url",
                    userInfo.getOrDefault("avatar_large",
                            userInfo.getOrDefault("profile_image_url",
                                    userInfo.getOrDefault("avatar", null))));
        }

        // 3. 查找或创建用户
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));
        if (user == null && unionid != null) {
            user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUnionid, unionid));
        }

        if (user == null) {
            // 自动注册
            user = new User();
            user.setUsername(platform + "_" + openid.substring(0, Math.min(openid.length(), 8)));
            user.setNickname(nickname != null ? nickname : platform + "用户");
            user.setAvatar(avatar);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setStatus(1);
            user.setRegisterSource(platform);
            save(user);
        } else {
            // 更新用户信息
            if (nickname != null) user.setNickname(nickname);
            if (avatar != null) user.setAvatar(avatar);
            if (unionid != null) user.setUnionid(unionid);
            updateById(user);
        }

        checkUserStatus(user);
        updateLoginInfo(user);
        return buildLoginResult(user);
    }

    /**
     * 旧版社交登录兼容（直接传入 openid）
     */
    @Transactional
    public Map<String, Object> loginBySocial(String platform, String openid, String unionid,
                                              String nickname, String avatar) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getOpenid, openid));
        if (user == null && unionid != null) {
            user = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUnionid, unionid));
        }

        if (user == null) {
            user = new User();
            user.setUsername(platform + "_" + openid.substring(0, Math.min(openid.length(), 8)));
            user.setNickname(nickname != null ? nickname : platform + "用户");
            user.setAvatar(avatar);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setStatus(1);
            user.setRegisterSource(platform);
            save(user);
        }

        checkUserStatus(user);
        updateLoginInfo(user);
        return buildLoginResult(user);
    }

    /**
     * 获取社交登录授权 URL
     */
    public String getSocialAuthorizeUrl(String platform, String redirectUri, String state) {
        SocialLoginStrategy strategy = socialLoginStrategyFactory.getStrategy(platform);
        return strategy.getAuthorizeUrl(redirectUri, state);
    }
}