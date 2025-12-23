package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * AI 日记详情 VO
 * 对应接口: GET /diary/{date}
 */
@Data
public class AiDiaryDetailVO {

    /**
     * 日记日期
     */
    @JsonProperty("diary_date")
    private String diaryDate;

    /**
     * 日记内容
     */
    @JsonProperty("diary_content")
    private String diaryContent;

    /**
     * 情绪标签列表
     */
    @JsonProperty("emotion_tags")
    private List<String> emotionTags;

    /**
     * 生成时间
     */
    @JsonProperty("generated_at")
    private String generatedAt;
}
