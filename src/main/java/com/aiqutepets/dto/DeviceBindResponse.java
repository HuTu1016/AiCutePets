package com.aiqutepets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备绑定响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceBindResponse {

    /**
     * 绑定是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 是否为设备管理员
     */
    private Boolean isOwner;

    /**
     * 设备 UID
     */
    private String deviceUid;

    /**
     * 产品型号
     */
    private String productModel;
}
