package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AI 日记日期列表项 VO
 * 对应接口: GET /diary/dates
 */
@Data
public class AiDiaryDateVO {

    /**
     * 日期
     */
    private String date;

    /**
     * 是否有日记
     */
    @JsonProperty("has_diary")
    private Boolean hasDiary;

    /**
     * 日记状态
     */
    @JsonProperty("diary_status")
    private String diaryStatus;
}
