package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * UniPush App 推送策略
 */
@Slf4j
@Component
public class UniPushNotifyStrategy implements NotifyStrategy {

    @Override
    public String getChannel() {
        return "unipush";
    }

    @Override
    public String getChannelName() {
        return "UniPush推送";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String content = replaceVariables(template.getContent(), params);
        String title = replaceVariables(template.getTitle(), params);
        log.info("UniPush推送: cid={}, title={}, content={}", target, title, content);
        // 实际生产环境使用 DCloud UniPush API
        return true;
    }

    private String replaceVariables(String template, Map<String, String> params) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}