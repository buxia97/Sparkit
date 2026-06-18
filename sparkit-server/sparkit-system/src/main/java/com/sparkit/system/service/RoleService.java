package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.system.mapper.RoleMapper;
import com.sparkit.system.mapper.RoleMenuMapper;
import com.sparkit.system.model.entity.Role;
import com.sparkit.system.model.entity.RoleMenu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleMenuMapper roleMenuMapper;

    @Transactional
    public void create(Role role) {
        if (count(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, role.getRoleName())) > 0) {
            throw new BusinessException(ErrorCode.ROLE_EXISTS);
        }
        if (count(new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, role.getRoleKey())) > 0) {
            throw new BusinessException(ErrorCode.ROLE_KEY_EXISTS);
        }
        save(role);
    }

    @Transactional
    public void update(Role role) {
        Role exist = getById(role.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!exist.getRoleName().equals(role.getRoleName())
                && count(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, role.getRoleName())) > 0) {
            throw new BusinessException(ErrorCode.ROLE_EXISTS);
        }
        updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = getById(id);
        if (role != null && role.getIsBuiltIn() != null && role.getIsBuiltIn() == 1) {
            throw new BusinessException(ErrorCode.ROLE_BUILT_IN);
        }
        removeById(id);
    }

    /** 分配菜单权限 */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    public List<Role> listAll() {
        return list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleSort));
    }
}