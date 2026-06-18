package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信公众号模板消息通知策略
 */
@Slf4j
@Component
public class WechatMpNotifyStrategy implements NotifyStrategy {

    @Override
    public String getChannel() {
        return "wechat_mp";
    }

    @Override
    public String getChannelName() {
        return "微信公众号通知";
    }

    @Override
    public boolean send(NotifyTemplate template, String target, Map<String, String> params) {
        String content = replaceVariables(template.getContent(), params);
        log.info("微信公众号模板消息发送: openid={}, templateId={}, content={}", target, template.getTemplateCode(), content);
        // 实际生产环境使用微信公众平台API
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