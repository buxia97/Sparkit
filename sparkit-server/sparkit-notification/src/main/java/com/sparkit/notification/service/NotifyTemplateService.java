package com.sparkit.notification.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sparkit.notification.mapper.NotifyTemplateMapper;
import com.sparkit.notification.model.entity.NotifyTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyTemplateService extends ServiceImpl<NotifyTemplateMapper, NotifyTemplate> {
}