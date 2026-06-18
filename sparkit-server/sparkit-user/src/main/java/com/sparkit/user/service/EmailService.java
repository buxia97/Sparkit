package com.sparkit.user.service;

import com.sparkit.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 邮箱服务
 * 支持：SMTP 通用邮箱、阿里云企业邮箱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ConfigService configService;

    /**
     * 发送验证码邮件
     */
    public boolean sendVerifyCode(String email, String code) {
        try {
            String host = configService.getConfigValue("email.smtp_host");
            String port = configService.getConfigValue("email.smtp_port");
            String username = configService.getConfigValue("email.username");
            String password = configService.getConfigValue("email.password");
            String from = configService.getConfigValue("email.from");
            String ssl = configService.getConfigValue("email.smtp_ssl");

            if (host == null || username == null || password == null) {
                log.warn("邮件服务未配置，仅打印验证码: email={}, code={}", email, code);
                return true;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port != null ? port : "465");
            props.put("mail.smtp.auth", "true");
            if ("true".equals(ssl)) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from != null ? from : username));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("验证码 - Sparkit");
            message.setContent(
                    "<div style='padding:20px;font-family:Arial,sans-serif;'>" +
                    "<h2>Sparkit 验证码</h2>" +
                    "<p>您的验证码是：<strong style='font-size:24px;color:#1890ff;'>" + code + "</strong></p>" +
                    "<p>验证码 5 分钟内有效，请勿泄露给他人。</p>" +
                    "<hr/><p style='color:#999;font-size:12px;'>此邮件由系统自动发送，请勿回复。</p></div>",
                    "text/html;charset=UTF-8"
            );

            Transport.send(message);
            log.info("邮件发送成功: email={}", email);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败: email={}", email, e);
            return false;
        }
    }
}