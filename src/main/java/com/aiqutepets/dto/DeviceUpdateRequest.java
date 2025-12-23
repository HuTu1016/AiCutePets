package com.aiqutepets.dto;

import lombok.Data;

/**
 * 设备信息更新请求
 */
@Data
public class DeviceUpdateRequest {

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 用户自定义设备昵称 (可选)
     */
    private String nickname;

    /**
     * 用户自定义设备头像 (可选)
     */
    private String avatar;
}
