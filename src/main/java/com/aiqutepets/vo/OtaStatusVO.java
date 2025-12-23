package com.aiqutepets.vo;

import com.aiqutepets.enums.OtaStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTA升级状态 VO（第三方接口响应解析后的视图对象）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OTA升级状态")
public class OtaStatusVO {

    @Schema(description = "设备UID")
    private String deviceUid;

    @Schema(description = "接口返回结果码 (1=成功, 其他=失败)")
    private Integer result;

    @Schema(description = "状态码: 0-无升级状态, 1-下载中, 2-下载完成, 3-下载失败, 4-升级中, 5-升级成功, 6-升级失败")
    private Integer status;

    @Schema(description = "状态描述文本")
    private String statusText;

    @Schema(description = "是否有新版本可升级 (result=1 且 status=0/1/2/3/4/6 且有版本号时为true)")
    private Boolean hasNewVersion;

    @Schema(description = "目标固件版本号")
    private String targetVersion;

    @Schema(description = "当前固件版本号")
    private String currentVersion;

    @Schema(description = "下载/升级进度百分比 (0-100)")
    private Integer progress;

    @Schema(description = "升级说明/更新日志")
    private String updateDescription;

    @Schema(description = "第三方接口原始响应报文（调试用）")
    private String rawResponse;

    /**
     * 根据状态码获取状态描述
     *
     * @return 状态描述文本
     */
    public String getStatusText() {
        if (this.statusText != null) {
            return this.statusText;
        }
        if (this.status != null) {
            return OtaStatusEnum.getDescription(this.status);
        }
        return "未知状态";
    }

    /**
     * 判断是否有新版本可升级
     * 规则：result=1 且 status 为 0/1/2/3/4/6（非升级成功） 且有目标版本号时为true
     *
     * @return 是否有新版本
     */
    public Boolean getHasNewVersion() {
        if (this.hasNewVersion != null) {
            return this.hasNewVersion;
        }
        if (this.result == null || this.result != 1) {
            return false;
        }
        if (this.status == null) {
            return false;
        }
        // 状态 5 为升级成功，无需再升级
        if (this.status == OtaStatusEnum.SUCCESS.getCode()) {
            return false;
        }
        // 有目标版本号才算有新版本
        return this.targetVersion != null && !this.targetVersion.isEmpty();
    }
}
