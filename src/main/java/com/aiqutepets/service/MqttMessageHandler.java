package com.aiqutepets.service;

import com.aiqutepets.entity.DeviceActionLog;
import com.aiqutepets.mapper.DeviceActionLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 消息处理器
 * 处理从 MQTT Broker 接收到的设备上报消息
 * 支持 up/{deviceId} 和 action/{deviceId} Topic
 */
@Slf4j
@Component
public class MqttMessageHandler implements MessageHandler {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDeviceRelService userDeviceRelService;

    @Autowired
    private DeviceActionLogMapper deviceActionLogMapper;

    /**
     * Redis Key 前缀 - 设备在线状态
     */
    private static final String DEVICE_ONLINE_KEY_PREFIX = "device:online:";

    /**
     * Redis Key 前缀 - 设备状态信息 (Hash)
     */
    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";

    /**
     * 设备在线状态过期时间（秒）- 120秒
     */
    private static final long ONLINE_EXPIRE_SECONDS = 120;

    /**
     * 设备状态过期时间（秒）- 300秒（5分钟）
     */
    private static final long STATUS_EXPIRE_SECONDS = 300;

    /**
     * 命令类型常量
     */
    private static final String CMD_ALIVE = "alive";
    private static final String CMD_UPLOAD_PARAM = "uploadParam";
    private static final String CMD_RESTORE_FACTORY = "restorefactory";

    /**
     * Topic 前缀常量
     */
    private static final String TOPIC_PREFIX_UP = "up/";
    private static final String TOPIC_PREFIX_ACTION = "action/";

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        String payload = message.getPayload().toString();

        log.trace("收到 MQTT 消息: topic={}, payload={}", topic, payload);

        try {
            // 1. 解析 Topic，提取 deviceId 和 topic 类型
            TopicInfo topicInfo = parseTopic(topic);
            if (topicInfo == null) {
                log.warn("无法解析 Topic: topic={}", topic);
                return;
            }

            String deviceId = topicInfo.deviceId;
            String topicType = topicInfo.topicType;

            // 2. 解析 Payload JSON
            JsonNode payloadNode = objectMapper.readTree(payload);

            // 3. 根据 Topic 类型分支处理
            if (TOPIC_PREFIX_UP.equals(topicType)) {
                // 处理 up/{deviceId} 消息
                handleUpMessage(deviceId, payloadNode);
            } else if (TOPIC_PREFIX_ACTION.equals(topicType)) {
                // 处理 action/{deviceId} 消息
                handleActionMessage(deviceId, payloadNode);
            }

        } catch (Exception e) {
            log.error("处理 MQTT 消息失败: topic={}, payload={}", topic, payload, e);
        }
    }

    /**
     * 解析 Topic，提取 deviceId 和 topic 类型
     */
    private TopicInfo parseTopic(String topic) {
        if (topic == null) {
            return null;
        }
        if (topic.startsWith(TOPIC_PREFIX_UP)) {
            return new TopicInfo(topic.substring(TOPIC_PREFIX_UP.length()), TOPIC_PREFIX_UP);
        }
        if (topic.startsWith(TOPIC_PREFIX_ACTION)) {
            return new TopicInfo(topic.substring(TOPIC_PREFIX_ACTION.length()), TOPIC_PREFIX_ACTION);
        }
        return null;
    }

    /**
     * Topic 解析结果
     */
    private static class TopicInfo {
        String deviceId;
        String topicType;

        TopicInfo(String deviceId, String topicType) {
            this.deviceId = deviceId;
            this.topicType = topicType;
        }
    }

    /**
     * 处理 up/{deviceId} 消息
     */
    private void handleUpMessage(String deviceId, JsonNode payloadNode) {
        String cmd = payloadNode.has("cmd") ? payloadNode.get("cmd").asText() : null;

        if (cmd == null) {
            log.warn("消息缺少 cmd 字段: deviceId={}", deviceId);
            return;
        }

        switch (cmd) {
            case CMD_ALIVE:
                handleHeartbeat(deviceId, payloadNode);
                break;
            case CMD_UPLOAD_PARAM:
                handleUploadParam(deviceId, payloadNode);
                break;
            case CMD_RESTORE_FACTORY:
                handleRestoreFactoryResponse(deviceId, payloadNode);
                break;
            default:
                log.info("未知命令类型: cmd={}, deviceId={}", cmd, deviceId);
        }
    }

    /**
     * 处理 action/{deviceId} 消息
     * 记录设备动作日志到数据库
     */
    private void handleActionMessage(String deviceId, JsonNode payloadNode) {
        String code = payloadNode.has("code") ? payloadNode.get("code").asText() : null;

        if (code == null) {
            log.warn("Action 消息缺少 code 字段: deviceId={}", deviceId);
            return;
        }

        log.info("收到设备动作消息: deviceId={}, code={}", deviceId, code);

        try {
            // 记录 DeviceActionLog 到数据库
            DeviceActionLog actionLog = DeviceActionLog.builder()
                    .deviceUid(deviceId)
                    .actionCode(code)
                    .durationOrCount(payloadNode.has("duration") ? payloadNode.get("duration").asInt() : null)
                    .createTime(LocalDateTime.now())
                    .build();

            deviceActionLogMapper.insert(actionLog);

            log.info("设备动作已记录: deviceId={}, code={}, logId={}", deviceId, code, actionLog.getId());

        } catch (Exception e) {
            log.error("记录设备动作日志失败: deviceId={}, code={}", deviceId, code, e);
        }

        // 同时更新设备在线状态
        updateDeviceOnline(deviceId);
    }

    /**
     * 处理心跳消息 (cmd=alive)
     * 更新 Redis: device:online:{deviceId} = true (设置 120 秒过期)
     */
    private void handleHeartbeat(String deviceId, JsonNode payloadNode) {
        log.trace("处理心跳消息: deviceId={}", deviceId);
        updateDeviceOnline(deviceId);
    }

    /**
     * 更新设备在线状态
     */
    private void updateDeviceOnline(String deviceId) {
        String redisKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
        stringRedisTemplate.opsForValue().set(redisKey, "true", ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.trace("设备在线状态已更新: key={}, expireSeconds={}", redisKey, ONLINE_EXPIRE_SECONDS);
    }

    /**
     * 处理参数上报消息 (cmd=uploadParam)
     * 提取 battery_level, volume_ratio, wifi_strength
     * 存入 Redis Hash: device:status:{deviceId}
     */
    private void handleUploadParam(String deviceId, JsonNode payloadNode) {
        log.debug("处理参数上报消息: deviceId={}", deviceId);

        String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceId;

        try {
            // 构造 Hash 数据
            Map<String, String> statusMap = new HashMap<>();
            statusMap.put("ts", String.valueOf(System.currentTimeMillis()));

            // 提取 battery_level (电量)
            if (payloadNode.has("battery_level")) {
                statusMap.put("battery_level", payloadNode.get("battery_level").asText());
            }

            // 提取 volume_ratio (音量)
            if (payloadNode.has("volume_ratio")) {
                statusMap.put("volume_ratio", payloadNode.get("volume_ratio").asText());
            }

            // 提取 wifi_strength (WiFi信号强度)
            if (payloadNode.has("wifi_strength")) {
                statusMap.put("wifi_strength", payloadNode.get("wifi_strength").asText());
            }

            // 提取其他可能的字段
            if (payloadNode.has("wifi_ssid")) {
                statusMap.put("wifi_ssid", payloadNode.get("wifi_ssid").asText());
            }
            if (payloadNode.has("voice_type")) {
                statusMap.put("voice_type", payloadNode.get("voice_type").asText());
            }

            // 存入 Redis Hash
            stringRedisTemplate.opsForHash().putAll(redisKey, statusMap);
            // 设置过期时间
            stringRedisTemplate.expire(redisKey, STATUS_EXPIRE_SECONDS, TimeUnit.SECONDS);

            log.debug("设备状态已更新(Hash): key={}, fields={}", redisKey, statusMap.keySet());

            // 同时更新设备在线状态
            updateDeviceOnline(deviceId);

        } catch (Exception e) {
            log.error("更新设备状态失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 处理恢复出厂设置回复 (cmd=restorefactory)
     * 如果 result == 1，调用 unbindAllByDevice 解除所有用户绑定
     */
    private void handleRestoreFactoryResponse(String deviceId, JsonNode payloadNode) {
        log.info("处理恢复出厂设置回复: deviceId={}", deviceId);

        int result = payloadNode.has("result") ? payloadNode.get("result").asInt() : -1;
        String msgId = payloadNode.has("msgId") ? payloadNode.get("msgId").asText() : null;

        if (result == 1) {
            log.info("收到设备重置确认，已自动解除所有用户绑定: {}", deviceId);

            try {
                // 调用 Service 解绑所有用户
                userDeviceRelService.unbindAllByDevice(deviceId);

                // 清除 Redis 中的设备状态
                String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
                String statusKey = DEVICE_STATUS_KEY_PREFIX + deviceId;
                stringRedisTemplate.delete(onlineKey);
                stringRedisTemplate.delete(statusKey);

                log.info("设备已解绑，Redis 状态已清除: deviceId={}, msgId={}", deviceId, msgId);

            } catch (Exception e) {
                log.error("解绑设备失败: deviceId={}", deviceId, e);
            }
        } else {
            log.warn("恢复出厂设置失败: deviceId={}, result={}, msgId={}", deviceId, result, msgId);
        }
    }
}
