package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AI 成长统计响应 VO
 * 对接接口: GET /api/devices/{device_uid}/stats/growth
 */
@Data
public class AiGrowthStatsVO {

    /**
     * 响应状态: success / error
     */
    private String status;

    /**
     * 成长统计数据
     */
    private GrowthStatsData data;

    /**
     * 成长统计数据内部类
     */
    @Data
    public static class GrowthStatsData {

        /**
         * 设备ID
         */
        @JsonProperty("device_id")
        private String deviceId;

        /**
         * 设备等级
         */
        @JsonProperty("device_level")
        private Integer deviceLevel;

        /**
         * 陪伴天数
         */
        @JsonProperty("accompany_days")
        private Integer accompanyDays;

        /**
         * 当前等级的各项数值
         */
        @JsonProperty("current_level_values")
        private LevelValues currentLevelValues;

        /**
         * 下一等级所需数值
         */
        @JsonProperty("next_level_requirements")
        private LevelValues nextLevelRequirements;
    }

    /**
     * 等级数值内部类
     */
    @Data
    public static class LevelValues {

        /**
         * 亲密值
         */
        @JsonProperty("intimacy_value")
        private Integer intimacyValue;

        /**
         * 陪伴值
         */
        @JsonProperty("companion_value")
        private Integer companionValue;

        /**
         * 情感值
         */
        @JsonProperty("emotion_value")
        private Integer emotionValue;

        /**
         * 喜爱值
         */
        @JsonProperty("affection_value")
        private Integer affectionValue;

        /**
         * 能量值
         */
        @JsonProperty("energy_value")
        private Integer energyValue;
    }
}
