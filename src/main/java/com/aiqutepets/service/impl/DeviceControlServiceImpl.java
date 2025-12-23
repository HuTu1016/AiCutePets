package com.aiqutepets.service.impl;

import com.aiqutepets.service.DeviceControlService;
import com.aiqutepets.service.MqttGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 设备控制服务实现类
 * 通过 MQTT Gateway 向设备发送控制指令 (文档 1.2.3)
 */
@Slf4j
@Service
public class DeviceControlServiceImpl implements DeviceControlService {

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Topic 前缀
     */
    private static final String TOPIC_PREFIX = "ctl/";

    /**
     * 命令类型常量
     */
    private static final String CMD_RESTORE_FACTORY = "restorefactory";
    private static final String CMD_UPDATE_START = "updatestart";

    @Override
    public void resetDevice(String deviceUid) {
        log.info("恢复出厂设置: deviceUid={}", deviceUid);

        try {
            // 构造 JSON: {"cmd": "restorefactory", "msgId": "..."}
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("cmd", CMD_RESTORE_FACTORY);
            payload.put("msgId", generateMsgId());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String topic = TOPIC_PREFIX + deviceUid;

            // 通过 Gateway 发送
            mqttGateway.sendToMqtt(topic, payloadJson);

            log.info("恢复出厂设置指令发送成功: deviceUid={}, topic={}, payload={}",
                    deviceUid, topic, payloadJson);

        } catch (Exception e) {
            log.error("发送恢复出厂设置指令失败: deviceUid={}", deviceUid, e);
            throw new RuntimeException("发送恢复出厂设置指令失败", e);
        }
    }

    @Override
    public void startOta(String deviceUid) {
        log.info("开始OTA升级: deviceUid={}", deviceUid);

        try {
            // 构造 JSON: {"cmd": "updatestart"}
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("cmd", CMD_UPDATE_START);
            payload.put("msgId", generateMsgId());

            String payloadJson = objectMapper.writeValueAsString(payload);
            String topic = TOPIC_PREFIX + deviceUid;

            // 通过 Gateway 发送
            mqttGateway.sendToMqtt(topic, payloadJson);

            log.info("OTA升级指令发送成功: deviceUid={}, topic={}, payload={}",
                    deviceUid, topic, payloadJson);

        } catch (Exception e) {
            log.error("发送OTA升级指令失败: deviceUid={}", deviceUid, e);
            throw new RuntimeException("发送OTA升级指令失败", e);
        }
    }

    @Override
    public void sendCommand(String deviceUid, String cmd, String payload) {
        log.info("发送自定义指令: deviceUid={}, cmd={}", deviceUid, cmd);

        try {
            String payloadJson;

            if (payload != null) {
                // 使用传入的 payload
                payloadJson = payload;
            } else {
                // 自动构造 payload
                ObjectNode payloadNode = objectMapper.createObjectNode();
                payloadNode.put("cmd", cmd);
                payloadNode.put("msgId", generateMsgId());
                payloadJson = objectMapper.writeValueAsString(payloadNode);
            }

            String topic = TOPIC_PREFIX + deviceUid;

            // 通过 Gateway 发送
            mqttGateway.sendToMqtt(topic, payloadJson);

            log.info("自定义指令发送成功: deviceUid={}, topic={}, cmd={}, payload={}",
                    deviceUid, topic, cmd, payloadJson);

        } catch (Exception e) {
            log.error("发送自定义指令失败: deviceUid={}, cmd={}", deviceUid, cmd, e);
            throw new RuntimeException("发送自定义指令失败", e);
        }
    }

    /**
     * 生成消息 ID
     *
     * @return 唯一的消息 ID
     */
    private String generateMsgId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
