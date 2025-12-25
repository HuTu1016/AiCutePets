package com.aiqutepets.service.impl;

import com.aiqutepets.dto.DeviceBindRequest;
import com.aiqutepets.dto.DeviceBindResponse;
import com.aiqutepets.dto.DeviceCheckResponse;
import com.aiqutepets.dto.DeviceStatusResponse;
import com.aiqutepets.dto.DeviceUpdateRequest;
import com.aiqutepets.dto.FirmwareCheckResponse;
import com.aiqutepets.dto.MyDeviceDTO;
import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.entity.UserDeviceRel;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.mapper.UserDeviceRelMapper;
import com.aiqutepets.service.DeviceManageService;
import com.aiqutepets.service.DeviceMqttService;
import com.aiqutepets.service.ThirdPartyOtaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备管理服务实现类
 */
@Slf4j
@Service
public class DeviceManageServiceImpl implements DeviceManageService {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private UserDeviceRelMapper userDeviceRelMapper;

    @Autowired
    private ThirdPartyOtaService thirdPartyOtaService;

    @Autowired
    private DeviceMqttService deviceMqttService;

    /**
     * 设备状态：未激活
     */
    private static final int DEVICE_STATUS_INACTIVE = 0;

    /**
     * 设备状态：已激活
     */
    private static final int DEVICE_STATUS_ACTIVE = 1;

    @Override
    public DeviceCheckResponse checkDeviceValid(String deviceUid) {
        log.info("校验设备合法性: deviceUid={}", deviceUid);

        DeviceInfo device = deviceInfoMapper.selectByDeviceUid(deviceUid);

        if (device == null) {
            log.warn("设备不存在: deviceUid={}", deviceUid);
            return DeviceCheckResponse.builder()
                    .valid(false)
                    .build();
        }

        log.info("设备存在: deviceUid={}, productModel={}", deviceUid, device.getProductModel());
        return DeviceCheckResponse.builder()
                .valid(true)
                .productModel(device.getProductModel())
                .status(device.getStatus())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceBindResponse bindDevice(Long userId, DeviceBindRequest request) {
        String deviceUid = request.getDeviceUid();
        log.info("用户 {} 请求绑定设备: deviceUid={}", userId, deviceUid);

        // 1. 查询设备是否存在，不存在则自动注册
        DeviceInfo device = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (device == null) {
            log.info("设备不存在，自动注册新设备: deviceUid={}", deviceUid);

            // 自动创建设备记录
            device = new DeviceInfo();
            device.setDeviceUid(deviceUid);
            device.setMac(request.getMacAddress()); // 从请求中获取 MAC 地址
            device.setSecretKey("auto_" + deviceUid); // 自动生成密钥（基于设备UID）
            device.setProductModel("DuoniTu"); // 默认产品型号
            device.setStatus(DEVICE_STATUS_ACTIVE); // 新绑定直接激活
            device.setCreateTime(LocalDateTime.now());
            device.setFirmwareVersion("1.0.0"); // 默认固件版本
            device.setOnlineStatus(0); // 默认离线

            deviceInfoMapper.insert(device);
            log.info("设备自动注册成功: deviceUid={}", deviceUid);
        }

        // 2. 检查用户是否已绑定该设备
        UserDeviceRel existingRel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (existingRel != null) {
            log.info("用户已绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return DeviceBindResponse.builder()
                    .success(true)
                    .message("已绑定该设备")
                    .isOwner(existingRel.getIsOwner() == 1)
                    .deviceUid(deviceUid)
                    .productModel(device.getProductModel())
                    .build();
        }

        // 3. 检查设备是否已被其他用户绑定
        List<UserDeviceRel> deviceBindings = userDeviceRelMapper.selectByDeviceUid(deviceUid);
        boolean hasOwner = deviceBindings.stream().anyMatch(rel -> rel.getIsOwner() == 1);

        if (hasOwner) {
            // 设备已有管理员，暂时返回错误（后续可改为添加普通成员）
            log.warn("设备已被其他用户绑定: deviceUid={}", deviceUid);
            return DeviceBindResponse.builder()
                    .success(false)
                    .message("设备已被其他用户绑定")
                    .build();
        }

        // 4. 创建绑定关系（设置为管理员，并设为当前设备）
        // 先清除该用户其他设备的 is_current 状态
        userDeviceRelMapper.clearCurrentDevice(userId);

        UserDeviceRel newRel = new UserDeviceRel();
        newRel.setUserId(userId);
        newRel.setDeviceUid(deviceUid);
        newRel.setIsOwner(1); // 首次绑定者为管理员
        newRel.setIsCurrent(1); // 新绑定的设备自动设为当前设备
        newRel.setBindSource("bluetooth");
        newRel.setCreateTime(LocalDateTime.now());
        userDeviceRelMapper.insert(newRel);

        log.info("创建设备绑定关系: userId={}, deviceUid={}, isOwner=1, isCurrent=1", userId, deviceUid);

        // 5. 更新设备状态为已激活
        if (device.getStatus() == DEVICE_STATUS_INACTIVE) {
            device.setStatus(DEVICE_STATUS_ACTIVE);
            // 如果传入了 MAC 地址，更新设备 MAC
            if (request.getMacAddress() != null && !request.getMacAddress().isEmpty()) {
                device.setMac(request.getMacAddress());
            }
            deviceInfoMapper.update(device);
            log.info("设备激活成功: deviceUid={}", deviceUid);
        }

        return DeviceBindResponse.builder()
                .success(true)
                .message("绑定成功")
                .isOwner(true)
                .deviceUid(deviceUid)
                .productModel(device.getProductModel())
                .build();
    }

    @Override
    public MyDeviceDTO getDeviceDetail(Long userId, String deviceUid) {
        log.info("获取设备详情: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验用户是否有权限查看该设备（通过联表查询同时完成权限校验）
        MyDeviceDTO deviceDetail = userDeviceRelMapper.selectMyDeviceDetail(userId, deviceUid);

        if (deviceDetail == null) {
            log.warn("用户无权访问该设备或设备不存在: userId={}, deviceUid={}", userId, deviceUid);
            return null;
        }

        // 2. 从 Redis 获取设备实时状态数据（MQTT 心跳数据）
        String statusJson = deviceMqttService.getDeviceRealtimeStatus(deviceUid);

        // 初始化默认状态
        boolean isOnline = false;
        int batteryLevel = deviceDetail.getBatteryLevel() != null ? deviceDetail.getBatteryLevel() : 0;
        int wifiSignalLevel = 0; // 0-4, 0无信号

        if (statusJson != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode statusNode = objectMapper.readTree(statusJson);

                // 检查 Redis MQTT 心跳时间
                long lastHeartbeatTime = statusNode.path("ts").asLong(0);
                long currentTime = System.currentTimeMillis();
                // 如果心跳在 60秒(60000ms) 内，视为在线
                if (currentTime - lastHeartbeatTime < 60000) {
                    isOnline = true;
                }

                // 获取电量
                batteryLevel = statusNode.path("bat").asInt(batteryLevel);

                // WiFi 信号强度获取
                // 通常设备上报的是 RSSI (如 -40 到 -90)
                // -50以上: 强(4), -50~-65: 中(3), -65~-80: 弱(2), -80以下: 极差(1)
                int rssi = statusNode.path("rssi").asInt(-100);
                if (isOnline) {
                    if (rssi > -50)
                        wifiSignalLevel = 4;
                    else if (rssi > -65)
                        wifiSignalLevel = 3;
                    else if (rssi > -80)
                        wifiSignalLevel = 2;
                    else
                        wifiSignalLevel = 1;
                }

            } catch (Exception e) {
                log.error("解析设备状态缓存失败: {}", deviceUid, e);
            }
        }

        // 3. 将实时状态更新到返回对象 (DTO)
        deviceDetail.setOnlineStatus(isOnline ? 1 : 0);
        deviceDetail.setBatteryLevel(batteryLevel);
        deviceDetail.setWifiSignalLevel(wifiSignalLevel);

        // 4. 如果用户没有设置 device_nickname，则默认返回产品型号作为名称
        if (deviceDetail.getDeviceNickname() == null || deviceDetail.getDeviceNickname().isEmpty()) {
            deviceDetail.setDeviceNickname(deviceDetail.getProductModel());
        }

        // 5. 计算已陪伴天数（从绑定时间到现在）
        if (deviceDetail.getBindTime() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    deviceDetail.getBindTime().toLocalDate(),
                    java.time.LocalDate.now());
            // 至少显示第1天
            deviceDetail.setCompanionDays(Math.max(1, days));
        } else {
            deviceDetail.setCompanionDays(1L);
        }

        log.info("设备详情获取成功: deviceUid={}, online={}, battery={}, companionDays={}",
                deviceUid, isOnline, batteryLevel, deviceDetail.getCompanionDays());
        return deviceDetail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDevice(Long userId, DeviceUpdateRequest request) {
        String deviceUid = request.getDeviceUid();
        log.info("更新设备信息: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验用户是否绑定了该设备
        UserDeviceRel rel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (rel == null) {
            log.warn("用户未绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return false;
        }

        // 2. 更新设备昵称和头像
        if (request.getNickname() != null) {
            rel.setDeviceNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            rel.setDeviceAvatar(request.getAvatar());
        }

        userDeviceRelMapper.update(rel);
        log.info("设备信息更新成功: deviceUid={}", deviceUid);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String unbindDevice(Long userId, String deviceUid) {
        log.info("解除设备绑定: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验用户是否绑定了该设备
        UserDeviceRel rel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (rel == null) {
            log.warn("用户未绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return "您未绑定该设备";
        }

        String message;
        if (rel.getIsOwner() != null && rel.getIsOwner() == 1) {
            // 管理员解绑：删除所有成员的绑定关系
            message = "管理员解绑成功，所有成员绑定关系已失效";
            log.info("管理员解绑设备，清理所有成员: userId={}, deviceUid={}", userId, deviceUid);
            
            // 删除该设备的所有绑定关系
            List<UserDeviceRel> allBindings = userDeviceRelMapper.selectByDeviceUid(deviceUid);
            for (UserDeviceRel binding : allBindings) {
                userDeviceRelMapper.deleteById(binding.getId());
            }
            log.info("已删除设备所有绑定关系: deviceUid={}, count={}", deviceUid, allBindings.size());
        } else {
            // 普通成员解绑：只删除自己的绑定
            message = "解绑成功";
            userDeviceRelMapper.deleteByUserIdAndDeviceUid(userId, deviceUid);
            log.info("普通成员解绑成功: userId={}, deviceUid={}", userId, deviceUid);
        }

        return message;
    }

    @Override
    public FirmwareCheckResponse checkFirmwareUpdate(Long userId, String deviceUid) {
        log.info("检查固件更新: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验用户是否绑定了该设备
        UserDeviceRel rel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (rel == null) {
            log.warn("用户未绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return null;
        }

        // 2. 获取设备的 secret_key
        DeviceInfo device = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (device == null || device.getSecretKey() == null) {
            log.warn("设备不存在或密钥为空: deviceUid={}", deviceUid);
            return FirmwareCheckResponse.builder()
                    .hasNewVersion(false)
                    .statusDesc("设备信息不完整")
                    .build();
        }

        // 3. 调用第三方 OTA 服务检查固件更新
        return thirdPartyOtaService.checkFirmwareUpdate(deviceUid, device.getSecretKey());
    }

    @Override
    public DeviceStatusResponse refreshDeviceStatus(Long userId, String deviceUid) {
        log.info("刷新设备状态: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验用户是否绑定了该设备
        UserDeviceRel rel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (rel == null) {
            log.warn("用户未绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return null;
        }

        // 2. 获取设备信息（作为兜底数据）
        DeviceInfo device = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (device == null) {
            log.warn("设备不存在: deviceUid={}", deviceUid);
            return null;
        }

        // 3. 优先从 Redis 获取实时状态（MQTT 心跳数据）
        Integer onlineStatus;
        Integer batteryLevel = device.getBatteryLevel();
        Integer wifiSignalLevel = 0;
        java.time.LocalDateTime lastActiveTime = device.getLastActiveTime();

        String realtimeStatus = deviceMqttService.getDeviceRealtimeStatus(deviceUid);
        if (realtimeStatus != null) {
            // Redis 中有数据，说明设备在线（5分钟内有心跳）
            onlineStatus = 1;
            try {
                com.fasterxml.jackson.databind.JsonNode statusNode = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(realtimeStatus);

                // 解析电量
                if (statusNode.has("bat")) {
                    batteryLevel = statusNode.get("bat").asInt();
                }

                // 解析 RSSI 并转换为信号等级
                if (statusNode.has("rssi")) {
                    int rssi = statusNode.get("rssi").asInt();
                    wifiSignalLevel = rssiToSignalLevel(rssi);
                }

                // 解析时间戳
                if (statusNode.has("ts")) {
                    long ts = statusNode.get("ts").asLong();
                    lastActiveTime = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(ts),
                            java.time.ZoneId.systemDefault());
                }
            } catch (Exception e) {
                log.warn("解析 Redis 状态数据失败: {}", realtimeStatus, e);
            }
        } else {
            // Redis 中无数据，说明设备离线
            onlineStatus = 0;
        }

        // 4. 生成状态描述
        String onlineStatusDesc = onlineStatus == 1 ? "在线" : "离线";
        String wifiSignalDesc = getWifiSignalDesc(wifiSignalLevel);

        log.info("设备状态刷新完成: deviceUid={}, onlineStatus={}, batteryLevel={}, wifiSignalLevel={}",
                deviceUid, onlineStatus, batteryLevel, wifiSignalLevel);

        return DeviceStatusResponse.builder()
                .deviceUid(deviceUid)
                .onlineStatus(onlineStatus)
                .onlineStatusDesc(onlineStatusDesc)
                .wifiSignalLevel(wifiSignalLevel)
                .wifiSignalDesc(wifiSignalDesc)
                .batteryLevel(batteryLevel)
                .lastActiveTime(lastActiveTime)
                .build();
    }

    /**
     * 将 RSSI 值转换为信号等级
     * RSSI 范围通常为 -30 到 -100 dBm
     *
     * @param rssi RSSI 值
     * @return 信号等级 0-3
     */
    private int rssiToSignalLevel(int rssi) {
        if (rssi >= -50) {
            return 3; // 强
        } else if (rssi >= -70) {
            return 2; // 中
        } else if (rssi >= -90) {
            return 1; // 弱
        } else {
            return 0; // 无信号
        }
    }

    /**
     * 获取 WiFi 信号强度描述
     *
     * @param level 信号等级
     * @return 信号描述
     */
    private String getWifiSignalDesc(Integer level) {
        if (level == null || level == 0) {
            return "无信号";
        }
        switch (level) {
            case 1:
                return "弱";
            case 2:
                return "中";
            case 3:
                return "强";
            default:
                return "未知";
        }
    }

    @Override
    public java.util.List<com.aiqutepets.dto.DeviceListDTO> getDeviceList(Long userId) {
        log.info("获取设备列表: userId={}", userId);

        // 1. 从数据库获取设备列表
        java.util.List<com.aiqutepets.dto.DeviceListDTO> deviceList = userDeviceRelMapper.selectDeviceList(userId);

        if (deviceList == null || deviceList.isEmpty()) {
            log.info("用户没有绑定设备: userId={}", userId);
            return java.util.Collections.emptyList();
        }

        // 2. 遍历列表，从 Redis 获取每台设备的实时状态
        for (com.aiqutepets.dto.DeviceListDTO device : deviceList) {
            String statusJson = deviceMqttService.getDeviceRealtimeStatus(device.getDeviceUid());

            if (statusJson != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode statusNode = objectMapper.readTree(statusJson);

                    // 检查心跳时间判断在线状态
                    long lastHeartbeatTime = statusNode.path("ts").asLong(0);
                    long currentTime = System.currentTimeMillis();
                    boolean isOnline = (currentTime - lastHeartbeatTime) < 60000; // 60秒内有心跳视为在线

                    device.setIsOnline(isOnline);

                    // 获取电量
                    if (statusNode.has("bat")) {
                        device.setBatteryLevel(statusNode.get("bat").asInt());
                    }
                } catch (Exception e) {
                    log.warn("解析设备状态失败: deviceUid={}", device.getDeviceUid(), e);
                }
            } else {
                // Redis 无数据，设备离线
                device.setIsOnline(false);
            }
        }

        log.info("设备列表获取成功: userId={}, count={}", userId, deviceList.size());
        return deviceList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean switchDevice(Long userId, String deviceUid) {
        log.info("切换当前设备: userId={}, deviceUid={}", userId, deviceUid);

        // 1. 校验该设备是否归属当前用户
        UserDeviceRel rel = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (rel == null) {
            log.warn("用户未绑定该设备: userId={}, deviceUid={}", userId, deviceUid);
            return false;
        }

        // 2. 重置当前设备状态（将该用户所有设备的 is_current 设为 0）
        userDeviceRelMapper.clearCurrentDevice(userId);

        // 3. 设置新的当前设备
        userDeviceRelMapper.setCurrentDevice(userId, deviceUid);

        log.info("设备切换成功: userId={}, deviceUid={}", userId, deviceUid);
        return true;
    }
}
