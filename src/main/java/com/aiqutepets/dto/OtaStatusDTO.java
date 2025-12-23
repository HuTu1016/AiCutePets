package com.aiqutepets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA升级状态响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTA升级状态响应")
public class OtaStatusDTO {

    @Schema(description = "设备UID")
    private String deviceUid;

    @Schema(description = "状态码: 0-无升级状态, 1-下载中, 2-下载完成, 3-下载失败, 4-升级中, 5-升级成功, 6-升级失败")
    private Integer statusCode;

    @Schema(description = "状态描述文本")
    private String statusText;

    @Schema(description = "是否有新版本可升级 (result=1 且 status=0/1/2/3/4/6 且有版本号时为true)")
    private Boolean hasNewVersion;

    @Schema(description = "目标固件版本号")
    private String targetVersion;
}
