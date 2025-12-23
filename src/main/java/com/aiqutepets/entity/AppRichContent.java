package com.aiqutepets.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * App 富文本内容表
 */
@Data
public class AppRichContent {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 内容标识(唯一Key): guide-玩伴指南, agreement-陪伴约定
     */
    private String contentKey;

    /**
     * 标题
     */
    private String title;

    /**
     * 富文本内容(HTML格式)
     */
    private String contentHtml;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
