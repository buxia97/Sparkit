package com.sparkit.news.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.news.mapper.NewsCategoryMapper;
import com.sparkit.news.model.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 新闻分类服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCategoryService extends ServiceImpl<NewsCategoryMapper, NewsCategory> {

    public List<NewsCategory> getTree() {
        List<NewsCategory> all = list(new LambdaQueryWrapper<NewsCategory>()
                .eq(NewsCategory::getStatus, 1)
                .orderByAsc(NewsCategory::getSort));
        return buildTree(all, 0L);
    }

    private List<NewsCategory> buildTree(List<NewsCategory> list, Long parentId) {
        return list.stream()
                .filter(c -> parentId.equals(c.getParentId() != null ? c.getParentId() : 0L))
                .peek(c -> c.setChildren(buildTree(list, c.getId())))
                .collect(Collectors.toList());
    }
}