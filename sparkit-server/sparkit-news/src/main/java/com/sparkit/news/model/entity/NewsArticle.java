package com.sparkit.news.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sparkit.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 新闻文章
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sparkit_news_article")
public class NewsArticle extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String source;
    private String sourceUrl;
    private String author;
    private Integer isTop;
    private Integer isRecommend;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer status;
    private Integer aiGenerated;
    private LocalDateTime publishTime;
    @TableLogic
    private Integer deleted;
}