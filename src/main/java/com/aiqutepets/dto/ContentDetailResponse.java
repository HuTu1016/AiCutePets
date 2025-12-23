package com.aiqutepets.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 富文本内容响应
 */
@Data
@Builder
public class ContentDetailResponse {

    /**
     * 标题
     */
    private String title;

    /**
     * 富文本内容 (HTML 格式)
     */
    private String content;
}
