package com.aiqutepets.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备状态刷新响应
 */
@Data
@Builder
public class DeviceStatusResponse {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 在线状态: 0-离线 1-在线
     */
    private Integer onlineStatus;

    /**
     * 在线状态描述
     */
    private String onlineStatusDesc;

    /**
     * WiFi 信号强度等级: 0-无信号 1-弱 2-中 3-强
     */
    private Integer wifiSignalLevel;

    /**
     * WiFi 信号强度描述
     */
    private String wifiSignalDesc;

    /**
     * 剩余电量 0-100
     */
    private Integer batteryLevel;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
}
