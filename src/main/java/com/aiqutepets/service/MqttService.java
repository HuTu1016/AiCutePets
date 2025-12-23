package com.aiqutepets.service;

/**
 * MQTT 消息服务接口
 * 用于向 MQTT Broker 发送消息
 */
public interface MqttService {

    /**
     * 向指定 Topic 发送消息
     *
     * @param topic   目标 Topic
     * @param payload 消息内容
     */
    void sendMessage(String topic, String payload);

    /**
     * 向默认 Topic 发送消息
     *
     * @param payload 消息内容
     */
    void sendMessage(String payload);

    /**
     * 向指定设备发送控制消息
     * Topic 格式: ctl/{deviceUid}
     *
     * @param deviceUid 设备唯一标识
     * @param payload   消息内容
     */
    void sendControlMessage(String deviceUid, String payload);
}
