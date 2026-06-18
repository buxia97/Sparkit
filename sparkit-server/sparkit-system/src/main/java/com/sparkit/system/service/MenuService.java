package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.constant.Constants;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.system.mapper.MenuMapper;
import com.sparkit.system.model.entity.Menu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    @Transactional
    public void update(Menu menu) {
        Menu exist = getById(menu.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        updateById(menu);
    }

    @Transactional
    public void delete(Long id) {
        if (count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        removeById(id);
    }

    /**
     * 获取菜单树
     */
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = list(new LambdaQueryWrapper<Menu>().orderByAsc(Menu::getSort));
        return buildTree(allMenus, 0L);
    }

    /**
     * 获取用户菜单树
     */
    public List<Menu> getUserMenuTree(Long userId) {
        List<Menu> menus = baseMapper.selectMenusByUserId(userId);
        return buildTree(menus, 0L);
    }

    private List<Menu> buildTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .peek(m -> m.setChildren(buildTree(menus, m.getId())))
                .collect(Collectors.toList());
    }

    /** 获取所有菜单权限标识 */
    public Set<String> getAllPerms() {
        return list(new LambdaQueryWrapper<Menu>()
                .isNotNull(Menu::getPerms)
                .ne(Menu::getPerms, "")
                .eq(Menu::getStatus, 1))
                .stream()
                .map(Menu::getPerms)
                .collect(Collectors.toSet());
    }
}