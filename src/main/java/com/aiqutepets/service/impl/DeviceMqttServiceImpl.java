package com.aiqutepets.service.impl;

import com.aiqutepets.service.DeviceMqttService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 设备 MQTT 消息处理服务实现类
 */
@Slf4j
@Service
public class DeviceMqttServiceImpl implements DeviceMqttService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Redis Key 前缀
     */
    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";

    /**
     * 设备状态过期时间（分钟）
     * 如果 5 分钟没有心跳，Redis key 消失，自然判定为离线
     */
    private static final long STATUS_EXPIRE_MINUTES = 5;

    @Override
    public void handleDeviceStatusMessage(String deviceUid, String payload) {
        log.info("收到设备状态上报: deviceUid={}, payload={}", deviceUid, payload);

        try {
            // 1. 解析 Payload JSON
            JsonNode payloadNode = objectMapper.readTree(payload);

            // 2. 获取当前系统时间戳
            long timestamp = System.currentTimeMillis();

            // 3. 构造新的 JSON 对象，包含时间戳
            ObjectNode statusNode = objectMapper.createObjectNode();
            statusNode.put("ts", timestamp);

            // 复制原始 payload 中的字段
            if (payloadNode.has("bat")) {
                statusNode.put("bat", payloadNode.get("bat").asInt());
            }
            if (payloadNode.has("rssi")) {
                statusNode.put("rssi", payloadNode.get("rssi").asInt());
            }

            // 也可以保留其他可能的字段
            payloadNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!"bat".equals(key) && !"rssi".equals(key)) {
                    statusNode.set(key, entry.getValue());
                }
            });

            // 4. 转换为 JSON 字符串
            String statusJson = objectMapper.writeValueAsString(statusNode);

            // 5. 存入 Redis，Key 为 device:status:{uid}
            String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceUid;
            stringRedisTemplate.opsForValue().set(redisKey, statusJson, STATUS_EXPIRE_MINUTES, TimeUnit.MINUTES);

            log.info("设备状态已更新到 Redis: key={}, value={}, expireMinutes={}",
                    redisKey, statusJson, STATUS_EXPIRE_MINUTES);

        } catch (Exception e) {
            log.error("处理设备状态消息失败: deviceUid={}, payload={}", deviceUid, payload, e);
        }
    }

    @Override
    public String getDeviceRealtimeStatus(String deviceUid) {
        String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceUid;
        String status = stringRedisTemplate.opsForValue().get(redisKey);
        log.debug("获取设备实时状态: deviceUid={}, status={}", deviceUid, status);
        return status;
    }

    @Override
    public boolean isDeviceOnline(String deviceUid) {
        String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceUid;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        boolean online = Boolean.TRUE.equals(exists);
        log.debug("检查设备在线状态: deviceUid={}, online={}", deviceUid, online);
        return online;
    }
}
