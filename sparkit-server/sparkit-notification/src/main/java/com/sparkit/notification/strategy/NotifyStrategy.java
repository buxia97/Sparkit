package com.sparkit.notification.strategy;

import com.sparkit.notification.model.entity.NotifyTemplate;

import java.util.Map;

/**
 * 通知发送策略接口
 * 所有通知渠道（邮件/短信/公众号/UniPush/站内信）必须实现此接口
 */
public interface NotifyStrategy {

    /** 获取通知渠道 */
    String getChannel();

    /** 获取渠道名称 */
    String getChannelName();

    /**
     * 发送通知
     * @param template 通知模板
     * @param target 发送目标（邮箱/手机号/openid等）
     * @param params 模板变量替换参数
     * @return 是否发送成功
     */
    boolean send(NotifyTemplate template, String target, Map<String, String> params);
}