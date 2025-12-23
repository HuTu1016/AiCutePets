package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AI 心情接口响应 VO
 * 对应接口: GET /api/devices/{device_uid}/mood/today
 */
@Data
public class AiMoodVO {

    /**
     * 响应状态: success / error
     */
    private String status;

    /**
     * 心情数据
     */
    private MoodData data;

    /**
     * 心情数据内部类
     */
    @Data
    public static class MoodData {

        /**
         * 设备ID
         */
        @JsonProperty("device_id")
        private String deviceId;

        /**
         * 心情日期
         */
        @JsonProperty("mood_date")
        private String moodDate;

        /**
         * 心情内容
         */
        @JsonProperty("mood_content")
        private String moodContent;

        /**
         * 生成时间
         */
        @JsonProperty("generated_at")
        private String generatedAt;
    }
}
