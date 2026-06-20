package com.sparkit.notification.controller;

import lombok.Data;

/**
 * 发送站内信请求
 */
@Data
public class SendMessageRequest {
    private Long receiverId;
    private String title;
    private String content;
    private String contentType;
}