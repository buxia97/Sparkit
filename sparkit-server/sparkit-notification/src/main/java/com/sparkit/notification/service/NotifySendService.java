package com.sparkit.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sparkit.notification.model.entity.NotifyRecord;
import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.notification.strategy.NotifyStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知发送服务
 */
@Slf4j
@Service
public class NotifySendService {

    private final Map<String, NotifyStrategy> strategyMap;
    private final NotifyRecordService recordService;
    private final NotifyTemplateService templateService;

    public NotifySendService(List<NotifyStrategy> strategies, NotifyRecordService recordService,
                             NotifyTemplateService templateService) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(NotifyStrategy::getChannel, s -> s));
        this.recordService = recordService;
        this.templateService = templateService;
    }

    @Transactional
    public boolean send(String templateCode, String target, Map<String, String> params) {
        NotifyTemplate template = templateService.getOne(
                new LambdaQueryWrapper<NotifyTemplate>().eq(NotifyTemplate::getTemplateCode, templateCode));
        if (template == null) {
            log.warn("通知模板不存在: {}", templateCode);
            return false;
        }

        NotifyStrategy strategy = strategyMap.get(template.getNotifyType());
        if (strategy == null) {
            log.warn("通知渠道策略不存在: {}", template.getNotifyType());
            return false;
        }

        boolean success = strategy.send(template, target, params);

        // 记录发送日志
        NotifyRecord record = new NotifyRecord();
        record.setTemplateCode(template.getTemplateCode());
        record.setNotifyType(template.getNotifyType());
        record.setTarget(target);
        record.setTitle(template.getTitle());
        record.setStatus(success ? 1 : 0);
        record.setSendTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        recordService.save(record);

        return success;
    }
}