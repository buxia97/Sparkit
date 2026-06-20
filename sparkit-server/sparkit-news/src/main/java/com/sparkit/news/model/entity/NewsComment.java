package com.sparkit.news.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新闻评论
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_news_comment")
public class NewsComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long articleId;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUserName;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Integer status;
    private Integer likeCount;
    private String ip;
    private String location;
    @TableLogic
    private Integer deleted;
}