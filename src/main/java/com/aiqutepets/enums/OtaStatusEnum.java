package com.aiqutepets.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OTA升级状态枚举
 */
@Getter
@AllArgsConstructor
public enum OtaStatusEnum {

    NONE(0, "无升级状态"),
    DOWNLOADING(1, "下载中"),
    DOWNLOAD_COMPLETE(2, "下载完成"),
    DOWNLOAD_FAIL(3, "下载失败"),
    UPGRADING(4, "升级中"),
    SUCCESS(5, "升级成功"),
    FAIL(6, "升级失败");

    private final int code;
    private final String description;

    /**
     * 根据状态码获取描述文本
     *
     * @param code 状态码
     * @return 描述文本，未知状态码返回"未知状态"
     */
    public static String getDescription(int code) {
        for (OtaStatusEnum status : values()) {
            if (status.code == code) {
                return status.description;
            }
        }
        return "未知状态";
    }

    /**
     * 根据状态码获取枚举实例
     *
     * @param code 状态码
     * @return 枚举实例，未知状态码返回null
     */
    public static OtaStatusEnum fromCode(int code) {
        for (OtaStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
