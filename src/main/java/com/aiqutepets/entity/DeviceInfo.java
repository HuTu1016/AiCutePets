package com.aiqutepets.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备白名单表
 */
@Data
public class DeviceInfo {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备唯一标识 (印在机身/二维码)
     */
    private String deviceUid;

    /**
     * 蓝牙MAC地址 (辅助校验)
     */
    private String mac;

    /**
     * 设备通信密钥 (用于签名)
     */
    private String secretKey;

    /**
     * 产品型号
     */
    private String productModel;

    /**
     * 状态: 0-未激活 1-已激活
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

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
}
