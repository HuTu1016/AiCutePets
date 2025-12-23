package com.aiqutepets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备合法性校验响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCheckResponse {

    /**
     * 设备是否合法
     */
    private Boolean valid;

    /**
     * 产品型号
     */
    private String productModel;

    /**
     * 设备状态
     */
    private Integer status;
}
