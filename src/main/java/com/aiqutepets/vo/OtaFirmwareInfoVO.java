package com.aiqutepets.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA最新固件信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTA最新固件信息")
public class OtaFirmwareInfoVO {

    @Schema(description = "接口返回结果码 (1=成功)")
    private Integer result;

    @Schema(description = "是否有新版本 (1:有 0:无)")
    @JsonProperty("isUpdate")
    private Integer isUpdate;

    @Schema(description = "最新版本号")
    private String version;

    @Schema(description = "更新日志/版本描述")
    private String description;

    @Schema(description = "发布日期")
    private String publishDate;

    @Schema(description = "固件文件大小(字节)")
    private Long fileSize;

    @Schema(description = "是否强制升级 (1:是 0:否)")
    @JsonProperty("isForce")
    private Integer isForce;

    @Schema(description = "第三方接口原始响应报文（调试用）")
    private String rawResponse;

    /**
     * 判断是否有可用更新
     *
     * @return true 表示有新版本可升级
     */
    public boolean hasUpdate() {
        return this.isUpdate != null && this.isUpdate == 1;
    }

    /**
     * 判断是否需要强制升级
     *
     * @return true 表示强制升级
     */
    public boolean isForceUpdate() {
        return this.isForce != null && this.isForce == 1;
    }

    /**
     * 获取格式化后的文件大小 (MB)
     *
     * @return 文件大小字符串，如 "1.5 MB"
     */
    public String getFileSizeFormatted() {
        if (this.fileSize == null || this.fileSize <= 0) {
            return "未知";
        }
        double mb = this.fileSize / (1024.0 * 1024.0);
        return String.format("%.2f MB", mb);
    }

    /**
     * 创建一个"无更新"的默认对象
     *
     * @return 无更新的 VO
     */
    public static OtaFirmwareInfoVO noUpdate() {
        return OtaFirmwareInfoVO.builder()
                .result(1)
                .isUpdate(0)
                .build();
    }
}
