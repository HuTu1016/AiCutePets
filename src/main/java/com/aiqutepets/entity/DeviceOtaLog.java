package com.aiqutepets.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备OTA升级记录表 (审计与排查用)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOtaLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备UID
     */
    private String deviceUid;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 目标版本号
     */
    private String targetVersion;

    /**
     * 操作类型: 1-发起检查 2-发起升级
     */
    private Integer actionType;

    /**
     * 第三方返回的状态码
     */
    private Integer statusCode;

    /**
     * 第三方返回的原始报文(用于排错)
     */
    private String apiResponse;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ========== 操作类型常量 ==========

    /**
     * 操作类型: 发起检查
     */
    public static final int ACTION_CHECK = 1;

    /**
     * 操作类型: 发起升级
     */
    public static final int ACTION_UPGRADE = 2;
}
