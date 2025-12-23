package com.aiqutepets.dto;

import lombok.Data;

/**
 * 设备绑定请求 DTO
 */
@Data
public class DeviceBindRequest {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 蓝牙 MAC 地址（可选）
     */
    private String macAddress;
}
