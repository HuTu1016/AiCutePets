package com.aiqutepets.dto;

import lombok.Data;

/**
 * 设备列表 DTO（用于首页设备切换列表）
 */
@Data
public class DeviceListDTO {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 设备昵称（如果为空则用产品名）
     */
    private String nickname;

    /**
     * 设备头像
     */
    private String avatar;

    /**
     * 是否当前选中设备
     */
    private Boolean isCurrent;

    /**
     * 是否在线（实时状态）
     */
    private Boolean isOnline;

    /**
     * 电量（实时电量 0-100）
     */
    private Integer batteryLevel;

    /**
     * 产品型号
     */
    private String productModel;

    /**
     * 蓝牙 MAC 地址
     */
    private String macAddress;
}
