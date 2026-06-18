package com.sparkit.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.system.mapper.PostMapper;
import com.sparkit.system.model.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 岗位服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService extends ServiceImpl<PostMapper, Post> {

    public PageResult<Post> page(PageQuery query) {
        Page<Post> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Post> result = page(page);
        return PageResult.of(result);
    }
}