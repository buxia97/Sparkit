package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件通知策略
 */
@Slf4j
@Component
public class EmailNotifyStrategy implements NotifyStrategy {

    @Override
    public String getChannel() {
        return "email";
    }

    @Override
    public String getChannelName() {
        return "邮件通知";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String content = replaceVariables(template.getContent(), params);
        String title = replaceVariables(template.getTitle(), params);
        log.info("邮件发送: to={}, title={}, content={}", target, title, content);
        // 实际生产环境使用 JavaMailSender 发送
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