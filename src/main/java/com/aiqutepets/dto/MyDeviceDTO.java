package com.aiqutepets.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户设备列表 DTO (包含设备详细信息)
 */
@Data
public class MyDeviceDTO {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

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
     * 是否管理员: 1-是 0-否
     */
    private Integer isOwner;

    /**
     * 蓝牙MAC地址
     */
    private String mac;

    /**
     * 产品型号
     */
    private String productModel;

    /**
     * 当前固件版本 (如 v1.0.0)
     */
    private String firmwareVersion;

    /**
     * 剩余电量 0-100
     */
    private Integer batteryLevel;

    /**
     * 在线状态: 0-离线 1-在线
     */
    private Integer onlineStatus;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;

    /**
     * WiFi 信号强度等级: 0-无信号 1-极差 2-弱 3-中 4-强
     */
    private Integer wifiSignalLevel;

    /**
     * 已陪伴天数（从绑定时间到现在）
     */
    private Long companionDays;
}
