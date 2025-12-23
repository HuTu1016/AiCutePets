package com.aiqutepets.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 固件更新状态响应
 */
@Data
@Builder
public class FirmwareCheckResponse {

    /**
     * 是否有新版本可用
     */
    private Boolean hasNewVersion;

    /**
     * 当前版本
     */
    private String currentVersion;

    /**
     * 最新版本
     */
    private String latestVersion;

    /**
     * 更新状态: 0-空闲 1-下载中 2-下载完成 3-下载失败 4-升级中 5-升级成功 6-升级失败
     */
    private Integer updateStatus;

    /**
     * 更新状态描述
     */
    private String statusDesc;

    /**
     * 更新进度 (0-100)
     */
    private Integer progress;

    /**
     * 更新说明
     */
    private String updateDescription;
}
