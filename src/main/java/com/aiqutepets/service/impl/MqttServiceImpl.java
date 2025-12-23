package com.aiqutepets.service.impl;

import com.aiqutepets.service.MqttService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

/**
 * MQTT 消息服务实现类
 * 通过 Spring Integration MQTT 发送消息到 MQTT Broker
 */
@Slf4j
@Service
public class MqttServiceImpl implements MqttService {

    @Autowired
    @Qualifier("mqttOutputChannel")
    private MessageChannel mqttOutputChannel;

    @Override
    public void sendMessage(String topic, String payload) {
        log.info("发送 MQTT 消息: topic={}, payload={}", topic, payload);

        try {
            mqttOutputChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build());
            log.info("MQTT 消息发送成功: topic={}", topic);
        } catch (Exception e) {
            log.error("MQTT 消息发送失败: topic={}, payload={}", topic, payload, e);
            throw new RuntimeException("MQTT 消息发送失败", e);
        }
    }

    @Override
    public void sendMessage(String payload) {
        log.info("发送 MQTT 消息到默认 Topic: payload={}", payload);

        try {
            mqttOutputChannel.send(
                    MessageBuilder.withPayload(payload).build());
            log.info("MQTT 消息发送成功（默认 Topic）");
        } catch (Exception e) {
            log.error("MQTT 消息发送失败: payload={}", payload, e);
            throw new RuntimeException("MQTT 消息发送失败", e);
        }
    }

    @Override
    public void sendControlMessage(String deviceUid, String payload) {
        String topic = "ctl/" + deviceUid;
        sendMessage(topic, payload);
    }
}
