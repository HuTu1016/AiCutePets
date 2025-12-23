package com.aiqutepets.service;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * MQTT 出站网关接口
 * 用于向 MQTT Broker 发送消息
 */
@MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
public interface MqttGateway {

    /**
     * 向指定 Topic 发送消息
     *
     * @param topic   目标 Topic
     * @param payload 消息内容
     */
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String payload);
}
