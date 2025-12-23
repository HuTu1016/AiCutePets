package com.aiqutepets.service;

/**
 * 设备控制服务接口
 * 用于向设备发送控制指令 (文档 1.2.3)
 */
public interface DeviceControlService {

    /**
     * 恢复出厂设置
     * Topic: ctl/{deviceUid}
     * Payload: {"cmd": "restorefactory", "msgId": "..."}
     *
     * @param deviceUid 设备唯一标识
     */
    void resetDevice(String deviceUid);

    /**
     * 开始 OTA 升级
     * Topic: ctl/{deviceUid}
     * Payload: {"cmd": "updatestart"}
     *
     * @param deviceUid 设备唯一标识
     */
    void startOta(String deviceUid);

    /**
     * 发送自定义指令
     *
     * @param deviceUid 设备唯一标识
     * @param cmd       命令类型
     * @param payload   完整的消息 JSON（如果为 null，则自动构造）
     */
    void sendCommand(String deviceUid, String cmd, String payload);
}
