package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * AI 徽章列表 VO
 * 对应接口: GET /stats/badges
 */
@Data
public class AiBadgeListVO {

    /**
     * 已解锁徽章列表
     */
    @JsonProperty("unlocked_badges")
    private List<BadgeItem> unlockedBadges;

    /**
     * 未解锁徽章列表
     */
    @JsonProperty("locked_badges")
    private List<BadgeItem> lockedBadges;

    /**
     * 徽章项内部类
     */
    @Data
    public static class BadgeItem {

        /**
         * 徽章代码
         */
        private String code;

        /**
         * 徽章名称
         */
        private String name;

        /**
         * 徽章描述
         */
        private String description;

        /**
         * 解锁时间
         */
        @JsonProperty("unlocked_at")
        private String unlockedAt;

        /**
         * 进度 (0-100)
         */
        private Integer progress;

        /**
         * 是否展示
         */
        @JsonProperty("is_shown")
        private Boolean isShown;
    }
}
