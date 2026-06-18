package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 短信通知策略（阿里云短信 / 飞鸽云短信）
 */
@Slf4j
@Component
public class SmsNotifyStrategy implements NotifyStrategy {

    @Override
    public String getChannel() {
        return "sms";
    }

    @Override
    public String getChannelName() {
        return "短信通知";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String content = replaceVariables(template.getContent(), params);
        log.info("短信发送: phone={}, content={}", target, content);
        // 实际生产环境使用阿里云短信SDK
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