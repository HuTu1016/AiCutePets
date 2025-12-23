package com.aiqutepets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA检查结果 DTO（前端展示用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTA固件检查结果")
public class OtaCheckResultDTO {

    @Schema(description = "设备UID")
    private String deviceUid;

    @Schema(description = "当前固件版本")
    private String currentVersion;

    @Schema(description = "最新可用版本")
    private String latestVersion;

    @Schema(description = "更新日志/版本描述")
    private String updateDesc;

    @Schema(description = "固件文件大小 (格式化后，如 '1.50 MB')")
    private String fileSize;

    @Schema(description = "发布日期")
    private String publishDate;

    @Schema(description = "当前状态码 (0-6)")
    private Integer status;

    @Schema(description = "状态描述文本 (如 '下载中 50%')")
    private String statusText;

    @Schema(description = "下载/升级进度百分比 (0-100)")
    private Integer progress;

    @Schema(description = "是否可以点击升级按钮")
    private Boolean canUpgrade;

    @Schema(description = "是否有新版本可用")
    private Boolean hasNewVersion;

    @Schema(description = "是否强制升级")
    private Boolean isForce;
}
