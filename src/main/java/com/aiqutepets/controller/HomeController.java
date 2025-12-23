package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.HomeIndexDTO;
import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.entity.UserDeviceRel;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.mapper.UserDeviceRelMapper;
import com.aiqutepets.service.DeviceMqttService;
import com.aiqutepets.service.ThirdPartyOtaService;
import com.aiqutepets.vo.AiGrowthStatsVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/home")
@Tag(name = "首页接口", description = "首页数据聚合接口，包含设备信息、成长数据、徽章状态等")
public class HomeController {

    /**
     * 目标徽章名称
     */
    private static final String TARGET_BADGE = "萌发的芽鞘";

    @Autowired
    private UserDeviceRelMapper userDeviceRelMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private ThirdPartyOtaService thirdPartyOtaService;

    @Autowired
    private DeviceMqttService deviceMqttService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取首页聚合数据
     *
     * @param userId 当前登录用户ID (JWT 解析)
     * @return 首页数据
     */
    @Operation(summary = "获取首页数据", description = "聚合首页所需数据：设备基础信息、AI成长数据（等级/进度）、徽章状态、五维数值、实时在线状态、电量、陪伴天数、今日心情文案。需要 JWT 鉴权")
    @GetMapping("/index")
    public Result<HomeIndexDTO> getHomeIndex(@RequestAttribute("userId") Long userId) {
        log.info("获取首页数据: userId={}", userId);

        // ============ 步骤 A: 获取当前设备 ============
        UserDeviceRel currentDevice = userDeviceRelMapper.selectCurrentDevice(userId);
        if (currentDevice == null) {
            log.warn("用户没有当前选中的设备: userId={}", userId);
            return Result.error("请先绑定并选择一台设备");
        }

        String deviceUid = currentDevice.getDeviceUid();
        log.info("当前选中设备: deviceUid={}", deviceUid);

        // 获取设备信息 (包含 secretKey 和 productModel)
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }

        String secretKey = deviceInfo.getSecretKey();
        String productModel = deviceInfo.getProductModel();

        // 准备返回数据，先用数据库缓存作为默认值
        Integer intimacyLevel = currentDevice.getIntimacyLevel() != null ? currentDevice.getIntimacyLevel() : 1;
        Integer intimacyScore = currentDevice.getIntimacyScore() != null ? currentDevice.getIntimacyScore() : 0;
        String currentBadge = currentDevice.getCurrentBadge();
        boolean newlyUnlocked = false;
        Map<String, Integer> stats = new HashMap<>();

        // ============ 步骤 B: 计算陪伴天数 ============
        Long accompanyDays = 1L; // 默认第1天
        if (currentDevice.getCreateTime() != null) {
            LocalDate bindDate = currentDevice.getCreateTime().toLocalDate();
            LocalDate today = LocalDate.now();
            long daysDiff = ChronoUnit.DAYS.between(bindDate, today);
            accompanyDays = daysDiff + 1; // 绑定当天算第1天
        }
        log.info("陪伴天数计算: deviceUid={}, days={}", deviceUid, accompanyDays);

        // ============ 步骤 C: 调用 AI 接口获取成长数据 ============
        try {
            AiGrowthStatsVO growthStats = thirdPartyOtaService.getDeviceGrowthStats(deviceUid, secretKey);

            if (growthStats != null && "success".equals(growthStats.getStatus()) && growthStats.getData() != null) {
                AiGrowthStatsVO.GrowthStatsData data = growthStats.getData();

                // 获取等级
                Integer aiLevel = data.getDeviceLevel();
                if (aiLevel != null) {
                    intimacyLevel = aiLevel;
                }

                // 计算亲密度百分比
                AiGrowthStatsVO.LevelValues currentValues = data.getCurrentLevelValues();
                AiGrowthStatsVO.LevelValues nextRequirements = data.getNextLevelRequirements();

                if (currentValues != null && nextRequirements != null) {
                    Integer currentIntimacy = currentValues.getIntimacyValue();
                    Integer nextIntimacy = nextRequirements.getIntimacyValue();

                    if (nextIntimacy == null || nextIntimacy == 0) {
                        // next 为 0 或 null，进度默认 100
                        intimacyScore = 100;
                    } else if (currentIntimacy != null) {
                        // 计算百分比，向下取整，最大 100
                        intimacyScore = Math.min(100, (int) ((currentIntimacy * 100.0) / nextIntimacy));
                    }

                    // 填充五维数据
                    stats.put("intimacy", currentValues.getIntimacyValue());
                    stats.put("companion", currentValues.getCompanionValue());
                    stats.put("emotion", currentValues.getEmotionValue());
                    stats.put("affection", currentValues.getAffectionValue());
                    stats.put("energy", currentValues.getEnergyValue());
                }

                // ============ 步骤 D: 徽章解锁判定 ============
                // 解锁条件: (level==1 && percentage>=100) || level>1
                boolean unlockCondition = (intimacyLevel == 1 && intimacyScore >= 100) || (intimacyLevel > 1);

                if (unlockCondition) {
                    // 如果 dbBadge 为空，表示首次解锁
                    String dbBadge = currentDevice.getCurrentBadge();
                    if (dbBadge == null || dbBadge.isEmpty()) {
                        currentBadge = TARGET_BADGE;
                        newlyUnlocked = true;
                        log.info("徽章解锁: deviceUid={}, badge={}", deviceUid, TARGET_BADGE);
                    } else {
                        currentBadge = dbBadge;
                    }
                }

                // 更新数据库缓存
                currentDevice.setIntimacyLevel(intimacyLevel);
                currentDevice.setIntimacyScore(intimacyScore);
                if (newlyUnlocked) {
                    currentDevice.setCurrentBadge(currentBadge);
                }

            } else {
                log.warn("AI 接口返回数据无效，使用数据库缓存: deviceUid={}", deviceUid);
            }

        } catch (Exception e) {
            log.error("调用 AI 接口失败，降级使用缓存数据: deviceUid={}", deviceUid, e);
        }

        // ============ 步骤 E: 获取今日心情 (Cache-Aside 策略) ============
        String dailyMood;
        LocalDate today = LocalDate.now();
        LocalDate cachedMoodDate = currentDevice.getLastMoodDate();
        String cachedMoodContent = currentDevice.getLastMoodContent();

        if (today.equals(cachedMoodDate) && cachedMoodContent != null && !cachedMoodContent.isEmpty()) {
            // 命中缓存：直接使用数据库里的文案
            dailyMood = cachedMoodContent;
            log.info("今日心情命中缓存: deviceUid={}", deviceUid);
        } else {
            // 需要刷新：调用 AI 接口获取新文案
            dailyMood = thirdPartyOtaService.getDeviceTodayMood(deviceUid, secretKey);
            // 更新数据库缓存
            currentDevice.setLastMoodDate(today);
            currentDevice.setLastMoodContent(dailyMood);
            log.info("今日心情已刷新: deviceUid={}, mood={}", deviceUid, dailyMood);
        }

        // ============ 步骤 F: 更新数据库 ============
        userDeviceRelMapper.update(currentDevice);
        log.info("更新设备数据缓存: deviceUid={}, level={}, score={}, badge={}",
                deviceUid, intimacyLevel, intimacyScore, currentBadge);

        // ============ 步骤 G: 从 Redis 获取实时状态 ============
        Boolean isOnline = false;
        Integer battery = 0;

        try {
            String statusJson = deviceMqttService.getDeviceRealtimeStatus(deviceUid);
            if (statusJson != null) {
                JsonNode statusNode = objectMapper.readTree(statusJson);
                isOnline = true; // Redis 有数据表示在线
                if (statusNode.has("bat")) {
                    battery = statusNode.get("bat").asInt();
                }
            }
        } catch (Exception e) {
            log.warn("获取设备实时状态失败: deviceUid={}", deviceUid, e);
        }

        // ============ 步骤 H: 组装返回 DTO ============
        String nickname = currentDevice.getDeviceNickname();
        if (nickname == null || nickname.isEmpty()) {
            nickname = productModel; // 使用产品型号作为默认昵称
        }

        HomeIndexDTO dto = HomeIndexDTO.builder()
                .deviceUid(deviceUid)
                .nickname(nickname)
                .avatar(currentDevice.getDeviceAvatar())
                .productModel(productModel)
                .intimacyLevel(intimacyLevel)
                .intimacyScore(intimacyScore)
                .currentBadge(currentBadge)
                .newlyUnlocked(newlyUnlocked)
                .stats(stats)
                .isOnline(isOnline)
                .battery(battery)
                .accompanyDays(accompanyDays)
                .dailyMood(dailyMood)
                .build();

        log.info("首页数据组装完成: deviceUid={}, level={}, score={}, online={}, days={}, mood={}",
                deviceUid, intimacyLevel, intimacyScore, isOnline, accompanyDays, dailyMood);

        return Result.success(dto);
    }
}
