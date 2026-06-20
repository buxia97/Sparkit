package com.sparkit.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.common.enums.ErrorCode;
import com.sparkit.common.exception.BusinessException;
import com.sparkit.common.model.PageQuery;
import com.sparkit.common.model.PageResult;
import com.sparkit.framework.security.SecurityContextHolder;
import com.sparkit.notification.mapper.SiteMessageMapper;
import com.sparkit.notification.model.entity.SiteMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内信服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteMessageService extends ServiceImpl<SiteMessageMapper, SiteMessage> {

    public PageResult<SiteMessage> page(PageQuery query, Integer isRead) {
        IPage<SiteMessage> page = new Page<>(query.getPage(), query.getPageSize());
        Long userId = SecurityContextHolder.getUserId();
        LambdaQueryWrapper<SiteMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SiteMessage::getReceiverId, userId);
        if (isRead != null) {
            wrapper.eq(SiteMessage::getIsRead, isRead);
        }
        wrapper.orderByDesc(SiteMessage::getCreateTime);
        IPage<SiteMessage> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public long unreadCount() {
        Long userId = SecurityContextHolder.getUserId();
        return lambdaQuery().eq(SiteMessage::getReceiverId, userId)
                .eq(SiteMessage::getIsRead, 0).count();
    }

    @Transactional
    public void read(Long id) {
        Long userId = SecurityContextHolder.getUserId();
        SiteMessage msg = getById(id);
        if (msg == null || !msg.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        msg.setIsRead(1);
        updateById(msg);
    }

    @Transactional
    public void readAll() {
        Long userId = SecurityContextHolder.getUserId();
        lambdaUpdate().eq(SiteMessage::getReceiverId, userId)
                .eq(SiteMessage::getIsRead, 0)
                .set(SiteMessage::getIsRead, 1)
                .update();
    }

    @Transactional
    public void send(Long receiverId, String title, String content, String contentType) {
        SiteMessage msg = new SiteMessage();
        msg.setSenderId(0L);
        msg.setSenderName("系统");
        msg.setReceiverId(receiverId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setContentType(contentType != null ? contentType : "text");
        msg.setStatus(1);
        msg.setIsRead(0);
        save(msg);
        log.info("站内信发送成功: receiverId={}, title={}", receiverId, title);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = SecurityContextHolder.getUserId();
        SiteMessage msg = getById(id);
        if (msg != null && msg.getReceiverId().equals(userId)) {
            removeById(id);
        }
    }
}