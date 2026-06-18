package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.sparkit.system.mapper.AdminUserMapper;
import com.sparkit.system.mapper.AdminUserRoleMapper;
import com.sparkit.system.model.entity.AdminUser;
import com.sparkit.system.model.entity.AdminUserRole;
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
 * 管理员用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService extends ServiceImpl<AdminUserMapper, AdminUser> {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public PageResult<AdminUser> page(PageQuery query) {
        IPage<AdminUser> page = new Page<>(query.getPage(), query.getPageSize());
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (query.getKeyword() != null) {
            wrapper.and(w -> w.like(AdminUser::getUsername, query.getKeyword())
                    .or().like(AdminUser::getNickname, query.getKeyword())
                    .or().like(AdminUser::getPhone, query.getKeyword()));
        }
        wrapper.orderByAsc(AdminUser::getCreateTime);
        IPage<AdminUser> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void create(AdminUser user) {
        // 校验用户名唯一
        if (exists(user.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        save(user);
    }

    @Transactional
    public void updateUser(AdminUser user) {
        AdminUser exist = getById(user.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 用户名唯一校验
        if (!exist.getUsername().equals(user.getUsername()) && exists(user.getUsername())) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
        updateById(user);
    }

    public boolean exists(String username) {
        return count(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)) > 0;
    }

    /**
     * 管理员登录
     */
    public Map<String, Object> login(String username, String password) {
        // 检查登录失败次数
        String failKey = CacheKeys.LOGIN_FAIL_COUNT + "admin:" + username;
        Integer failCount = (Integer) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount >= Constants.LOGIN_MAX_RETRY) {
            throw new BusinessException(ErrorCode.USER_LOCKED, Constants.LOGIN_LOCK_MINUTES);
        }

        AdminUser user = getOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 记录失败次数
            Long increment = redisTemplate.opsForValue().increment(failKey, 1);
            if (increment != null && increment == 1) {
                redisTemplate.expire(failKey, Constants.LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR);
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);

        // 获取权限
        Set<String> perms = baseMapper.selectPermsByUserId(user.getId());
        Set<Long> roleIds = baseMapper.selectRoleIdsByUserId(user.getId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUserType("admin");
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setPermissions(perms);
        loginUser.setRoleIds(roleIds);

        // 检查是否为超级管理员
        loginUser.setIsAdmin(roleIds != null && roleIds.contains(1L));

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", jwtTokenService.generateAccessToken(user.getId(), "admin", user.getUsername()));
        result.put("refreshToken", jwtTokenService.generateRefreshToken(user.getId(), "admin", user.getUsername()));
        result.put("userInfo", loginUser);
        return result;
    }

    /** 分配角色 */
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        adminUserRoleMapper.delete(new LambdaQueryWrapper<AdminUserRole>().eq(AdminUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            AdminUserRole ur = new AdminUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            adminUserRoleMapper.insert(ur);
        }
    }
}