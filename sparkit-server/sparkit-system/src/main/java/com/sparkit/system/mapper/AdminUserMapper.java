package com.sparkit.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sparkit.system.model.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

/**
 * 管理员用户 Mapper
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("SELECT DISTINCT m.perms FROM sparkit_menu m " +
            "INNER JOIN sparkit_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sparkit_admin_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms != '' AND m.deleted = 0")
    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT r.id FROM sparkit_role r " +
            "INNER JOIN sparkit_admin_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    Set<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}