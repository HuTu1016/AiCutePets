package com.aiqutepets.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MQTT 消息处理器
 * 处理从 MQTT Broker 接收到的设备上报消息
 */
@Slf4j
@Component
public class MqttMessageHandler implements MessageHandler {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Redis Key 前缀 - 设备在线状态
     */
    private static final String DEVICE_ONLINE_KEY_PREFIX = "device:online:";

    /**
     * Redis Key 前缀 - 设备状态信息
     */
    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";

    /**
     * 设备在线状态过期时间（分钟）
     */
    private static final long ONLINE_EXPIRE_MINUTES = 5;

    /**
     * 命令类型常量
     */
    private static final String CMD_ALIVE = "alive";
    private static final String CMD_UPLOAD_PARAM = "uploadParam";

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        String payload = message.getPayload().toString();

        log.info("收到 MQTT 消息: topic={}, payload={}", topic, payload);

        try {
            // 1. 从 Topic 中提取 deviceId (格式: up/{deviceId})
            String deviceId = extractDeviceId(topic);
            if (deviceId == null) {
                log.warn("无法从 Topic 提取 deviceId: topic={}", topic);
                return;
            }

            // 2. 解析 Payload JSON
            JsonNode payloadNode = objectMapper.readTree(payload);
            String cmd = payloadNode.has("cmd") ? payloadNode.get("cmd").asText() : null;

            if (cmd == null) {
                log.warn("消息缺少 cmd 字段: deviceId={}, payload={}", deviceId, payload);
                return;
            }

            // 3. 根据 cmd 类型分支处理
            switch (cmd) {
                case CMD_ALIVE:
                    handleHeartbeat(deviceId, payloadNode);
                    break;
                case CMD_UPLOAD_PARAM:
                    handleUploadParam(deviceId, payloadNode);
                    break;
                default:
                    log.info("未知命令类型: cmd={}, deviceId={}", cmd, deviceId);
            }

        } catch (Exception e) {
            log.error("处理 MQTT 消息失败: topic={}, payload={}", topic, payload, e);
        }
    }

    /**
     * 从 Topic 中提取 deviceId
     * Topic 格式: up/{deviceId}
     *
     * @param topic MQTT Topic
     * @return deviceId，如果格式不正确返回 null
     */
    private String extractDeviceId(String topic) {
        if (topic != null && topic.startsWith("up/")) {
            return topic.substring(3);
        }
        return null;
    }

    /**
     * 处理心跳消息 (cmd=alive)
     * 更新 Redis: device:online:{uid} = true (设置 5 分钟过期)
     *
     * @param deviceId    设备 ID
     * @param payloadNode 消息体 JSON
     */
    private void handleHeartbeat(String deviceId, JsonNode payloadNode) {
        log.info("处理心跳消息: deviceId={}", deviceId);

        String redisKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;

        // 设置设备在线状态，5 分钟过期
        stringRedisTemplate.opsForValue().set(redisKey, "true", ONLINE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("设备在线状态已更新: key={}, expireMinutes={}", redisKey, ONLINE_EXPIRE_MINUTES);
    }

    /**
     * 处理参数上报消息 (cmd=uploadParam)
     * 提取 battery_level, wifi_strength，更新 Redis: device:status:{uid}
     *
     * @param deviceId    设备 ID
     * @param payloadNode 消息体 JSON
     */
    private void handleUploadParam(String deviceId, JsonNode payloadNode) {
        log.info("处理参数上报消息: deviceId={}", deviceId);

        String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceId;

        // 获取之前的电量值（用于变化检测）
        Integer previousBatteryLevel = getPreviousBatteryLevel(redisKey);

        // 提取 battery_level 和 wifi_strength
        Integer batteryLevel = payloadNode.has("battery_level")
                ? payloadNode.get("battery_level").asInt()
                : null;
        Integer wifiStrength = payloadNode.has("wifi_strength")
                ? payloadNode.get("wifi_strength").asInt()
                : null;

        // 构造状态 JSON
        try {
            com.fasterxml.jackson.databind.node.ObjectNode statusNode = objectMapper.createObjectNode();
            statusNode.put("ts", System.currentTimeMillis());

            if (batteryLevel != null) {
                statusNode.put("battery_level", batteryLevel);
            }
            if (wifiStrength != null) {
                statusNode.put("wifi_strength", wifiStrength);
            }

            String statusJson = objectMapper.writeValueAsString(statusNode);

            // 存储到 Redis（5 分钟过期，与在线状态一致）
            stringRedisTemplate.opsForValue().set(redisKey, statusJson, ONLINE_EXPIRE_MINUTES, TimeUnit.MINUTES);

            log.info("设备状态已更新: key={}, batteryLevel={}, wifiStrength={}",
                    redisKey, batteryLevel, wifiStrength);

            // 检测电量变化并记录日志
            if (previousBatteryLevel != null && batteryLevel != null) {
                int diff = Math.abs(batteryLevel - previousBatteryLevel);
                if (diff >= 10) {
                    log.warn("设备电量变化较大: deviceId={}, 之前电量={}, 当前电量={}, 变化={}%",
                            deviceId, previousBatteryLevel, batteryLevel, diff);
                }
            }

            // 同时更新设备在线状态
            String onlineKey = DEVICE_ONLINE_KEY_PREFIX + deviceId;
            stringRedisTemplate.opsForValue().set(onlineKey, "true", ONLINE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("更新设备状态失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 获取之前的电量值
     *
     * @param redisKey Redis Key
     * @return 之前的电量值，如果不存在返回 null
     */
    private Integer getPreviousBatteryLevel(String redisKey) {
        try {
            String previousStatus = stringRedisTemplate.opsForValue().get(redisKey);
            if (previousStatus != null) {
                JsonNode node = objectMapper.readTree(previousStatus);
                if (node.has("battery_level")) {
                    return node.get("battery_level").asInt();
                }
            }
        } catch (Exception e) {
            log.debug("获取之前电量值失败: redisKey={}", redisKey);
        }
        return null;
    }
}
