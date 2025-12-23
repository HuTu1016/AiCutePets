package com.aiqutepets.service;

/**
 * 设备控制服务接口
 * 用于向设备发送控制指令
 */
public interface DeviceControlService {

    /**
     * 发送恢复出厂设置指令
     *
     * @param deviceUid 设备唯一标识
     */
    void sendResetCommand(String deviceUid);

    /**
     * 发送升级指令
     *
     * @param deviceUid 设备唯一标识
     */
    void sendUpgradeCommand(String deviceUid);

    /**
     * 发送自定义指令
     *
     * @param deviceUid 设备唯一标识
     * @param cmd       命令类型
     * @param payload   完整的消息 JSON（如果为 null，则自动构造）
     */
    void sendCommand(String deviceUid, String cmd, String payload);
}
