package com.sparkit.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.system.mapper.DeptMapper;
import com.sparkit.system.model.entity.Dept;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptService extends ServiceImpl<DeptMapper, Dept> {

    @Transactional
    public void delete(Long id) {
        if (count(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.DEPT_HAS_CHILDREN);
        }
        removeById(id);
    }

    public List<Dept> getDeptTree() {
        List<Dept> depts = list(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSort));
        return buildTree(depts, 0L);
    }

    private List<Dept> buildTree(List<Dept> depts, Long parentId) {
        return depts.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .peek(d -> d.setChildren(buildTree(depts, d.getId())))
                .collect(Collectors.toList());
    }
}