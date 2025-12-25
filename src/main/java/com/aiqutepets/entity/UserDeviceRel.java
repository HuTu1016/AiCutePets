package com.aiqutepets.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户设备绑定关系表
 */
@Data
public class UserDeviceRel {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID (mp_user.id)
     */
    private Long userId;

    /**
     * 设备UID
     */
    private String deviceUid;

    /**
     * 是否管理员: 1-是 0-否
     */
    private Integer isOwner;

    /**
     * 绑定来源
     */
    private String bindSource;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 用户自定义设备昵称
     */
    private String deviceNickname;

    /**
     * 用户自定义设备头像
     */
    private String deviceAvatar;

    /**
     * 是否置顶显示: 0-否 1-是
     */
    private Integer isTop;

    /**
     * 是否当前选中设备: 0-否 1-是
     */
    private Integer isCurrent;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedTime;

    /**
     * 亲密等级（AI返回的等级缓存）
     */
    private Integer intimacyLevel;

    /**
     * 亲密度百分比（0-100，缓存）
     */
    private Integer intimacyScore;

    /**
     * 当前徽章
     */
    private String currentBadge;

    /**
     * 五维成长数据缓存（JSON格式）
     */
    private String growthStatsJson;

    /**
     * 最后心情日期（用于缓存判断）
     */
    private java.time.LocalDate lastMoodDate;

    /**
     * 最后心情内容（缓存）
     */
    private String lastMoodContent;

    /**
     * OTA更新标记: 0-无更新, 1-有更新（用于首页红点缓存）
     */
    private Integer hasOtaUpdate;
}
