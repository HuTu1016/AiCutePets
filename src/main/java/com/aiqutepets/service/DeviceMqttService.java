package com.aiqutepets.service;

/**
 * 设备 MQTT 消息处理服务接口
 */
public interface DeviceMqttService {

    /**
     * 处理设备状态上报消息
     * 场景：设备定时上报心跳到 MQTT Topic device/{uid}/status
     *
     * @param deviceUid 设备唯一标识
     * @param payload   消息体 JSON，如 {"bat": 80, "rssi": -60}
     */
    void handleDeviceStatusMessage(String deviceUid, String payload);

    /**
     * 获取设备实时状态（从 Redis）
     *
     * @param deviceUid 设备唯一标识
     * @return 设备状态 JSON，如果不存在返回 null（表示离线）
     */
    String getDeviceRealtimeStatus(String deviceUid);

    /**
     * 判断设备是否在线（根据 Redis 中是否有心跳数据）
     *
     * @param deviceUid 设备唯一标识
     * @return true-在线 false-离线
     */
    boolean isDeviceOnline(String deviceUid);
}
