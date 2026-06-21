package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;
import com.sparkit.system.service.ConfigService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件通知策略（JavaMailSender 真实发送）
 */
@Slf4j
@Component
public class EmailNotifyStrategy implements NotifyStrategy {

    @Autowired(required = false)
    private JavaMailSender mailSender;
    private final ConfigService configService;

    public EmailNotifyStrategy(ConfigService configService) {
        this.configService = configService;
    }

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

        try {
            if (mailSender == null) {
                log.warn("邮件服务未配置，跳过发送: to={}", target);
                return false;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(getFrom());
            helper.setTo(target);
            helper.setSubject(title);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, title={}", target, title);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, error={}", target, e.getMessage());
            return false;
        }
    }

    private String getFrom() {
        String from = configService.getConfigValue("notification.email.from");
        return from != null ? from : "noreply@sparkit.com";
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