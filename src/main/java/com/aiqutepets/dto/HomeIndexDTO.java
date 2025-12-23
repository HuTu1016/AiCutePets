package com.aiqutepets.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 首页聚合数据响应 DTO
 */
@Data
@Builder
public class HomeIndexDTO {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 设备昵称
     */
    private String nickname;

    /**
     * 设备头像
     */
    private String avatar;

    /**
     * 产品型号
     */
    private String productModel;

    /**
     * 亲密等级
     */
    private Integer intimacyLevel;

    /**
     * 亲密度进度百分比 (0-100)
     */
    private Integer intimacyScore;

    /**
     * 当前徽章
     */
    private String currentBadge;

    /**
     * 是否刚刚解锁新徽章（用于前端弹窗）
     */
    private Boolean newlyUnlocked;

    /**
     * 五维数据统计
     * 包含: intimacy(亲密), companion(陪伴), emotion(情绪), affection(亲昵), energy(能量)
     */
    private Map<String, Integer> stats;

    /**
     * 是否在线
     */
    private Boolean isOnline;

    /**
     * 电量 (0-100)
     */
    private Integer battery;

    /**
     * 陪伴天数
     */
    private Long accompanyDays;

    /**
     * 今日心情文案
     */
    private String dailyMood;
}
