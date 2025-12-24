package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.OtaCheckResultDTO;
import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.entity.DeviceOtaLog;
import com.aiqutepets.entity.UserDeviceRel;
import com.aiqutepets.enums.OtaStatusEnum;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.mapper.DeviceOtaLogMapper;
import com.aiqutepets.mapper.UserDeviceRelMapper;
import com.aiqutepets.service.ThirdPartyOtaService;
import com.aiqutepets.vo.OtaFirmwareInfoVO;
import com.aiqutepets.vo.OtaStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * OTA固件升级控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/device/ota")
@Tag(name = "OTA固件升级", description = "设备OTA固件升级相关接口")
public class OtaController {

    @Autowired
    private UserDeviceRelMapper userDeviceRelMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private ThirdPartyOtaService thirdPartyOtaService;

    @Autowired
    private DeviceOtaLogMapper deviceOtaLogMapper;

    /**
     * 检查固件更新状态（聚合接口）
     * 
     * <p>
     * 同时查询当前升级状态和最新固件信息，返回综合结果
     * </p>
     *
     * @param userId    当前登录用户ID (JWT 解析)
     * @param deviceUid 设备UID
     * @return OTA检查结果
     */
    @Operation(summary = "检查固件详情", description = "查询指定设备的OTA升级状态，聚合当前状态和最新固件信息。需要JWT鉴权")
    @GetMapping("/check")
    public Result<OtaCheckResultDTO> checkOtaStatus(
            @RequestAttribute("currentUserId") Long userId,
            @Parameter(description = "设备UID", required = true) @RequestParam String deviceUid) {

        log.info("检查OTA固件状态: userId={}, deviceUid={}", userId, deviceUid);

        // ============ 步骤 A: 校验权限 ============
        UserDeviceRel userDevice = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (userDevice == null) {
            log.warn("用户无权访问该设备: userId={}, deviceUid={}", userId, deviceUid);
            return Result.error(403, "无权访问该设备");
        }

        // ============ 步骤 B: 获取设备信息 ============
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();
        String currentVersion = deviceInfo.getFirmwareVersion(); // 数据库中的当前版本

        // ============ 步骤 C: 查询当前升级状态 (0-6) ============
        OtaStatusVO otaStatus = thirdPartyOtaService.getOtaStatus(deviceUid, secretKey);
        Integer status = otaStatus.getStatus() != null ? otaStatus.getStatus() : 0;
        Integer progress = otaStatus.getProgress() != null ? otaStatus.getProgress() : 0;
        log.info("OTA状态查询结果: deviceUid={}, status={}, progress={}", deviceUid, status, progress);

        // ============ 步骤 D: 查询最新固件信息 ============
        OtaFirmwareInfoVO firmwareInfo = thirdPartyOtaService.getLatestFirmwareInfo(deviceUid, secretKey,
                currentVersion);
        log.info("最新固件信息: deviceUid={}, hasUpdate={}, version={}",
                deviceUid, firmwareInfo.hasUpdate(), firmwareInfo.getVersion());

        // ============ 步骤 E: 构建状态描述文本 ============
        String statusText = OtaStatusEnum.getDescription(status);
        // 如果正在下载或升级中，附加进度信息
        if (status == OtaStatusEnum.DOWNLOADING.getCode() || status == OtaStatusEnum.UPGRADING.getCode()) {
            statusText = statusText + " " + progress + "%";
        }

        // ============ 步骤 F: 判断是否可以升级 (canUpgrade) ============
        boolean canUpgrade = false;
        boolean hasNewVersion = firmwareInfo.hasUpdate();

        /*
         * canUpgrade 判断逻辑:
         * - 如果 status 为 1(下载中) 或 4(升级中) -> false (已经在升级了，不能点)
         * - 如果 status 为 0/3/6 且 firmwareInfo.isUpdate == 1 -> true (可以点升级)
         * - 其他情况 -> false
         */
        if (status == OtaStatusEnum.DOWNLOADING.getCode() || status == OtaStatusEnum.UPGRADING.getCode()) {
            // 正在下载或升级中，不能点
            canUpgrade = false;
        } else if ((status == OtaStatusEnum.NONE.getCode()
                || status == OtaStatusEnum.DOWNLOAD_FAIL.getCode()
                || status == OtaStatusEnum.FAIL.getCode())
                && hasNewVersion) {
            // 空闲/下载失败/升级失败 且有新版本，可以升级
            canUpgrade = true;
        }

        // ============ 步骤 G: 如果升级成功，自动更新数据库版本 ============
        if (status == OtaStatusEnum.SUCCESS.getCode()) {
            String latestVersion = firmwareInfo.getVersion();
            if (latestVersion != null && !latestVersion.isEmpty()) {
                try {
                    deviceInfo.setFirmwareVersion(latestVersion);
                    deviceInfoMapper.update(deviceInfo);
                    currentVersion = latestVersion; // 更新返回值
                    log.info("升级成功，已更新数据库固件版本: deviceUid={}, newVersion={}", deviceUid, latestVersion);
                } catch (Exception e) {
                    log.error("更新数据库固件版本失败: deviceUid={}", deviceUid, e);
                }
            }
        }

        // ============ 步骤 G+: 异步更新 OTA 红点缓存 (Smart Badge) ============
        // hasUpdate=true 且非升级成功 -> 1，否则 -> 0
        final boolean finalHasNewVersion = hasNewVersion;
        final boolean isUpgradeSuccess = (status == OtaStatusEnum.SUCCESS.getCode());
        final String finalDeviceUid = deviceUid;

        CompletableFuture.runAsync(() -> {
            try {
                int flagValue = (finalHasNewVersion && !isUpgradeSuccess) ? 1 : 0;
                userDeviceRelMapper.updateOtaUpdateFlag(finalDeviceUid, flagValue);
                log.debug("OTA红点缓存已更新: deviceUid={}, hasOtaUpdate={}", finalDeviceUid, flagValue);
            } catch (Exception e) {
                log.error("更新OTA红点缓存失败: deviceUid={}", finalDeviceUid, e);
            }
        });

        // ============ 步骤 H: 记录日志 ============
        try {
            DeviceOtaLog otaLog = DeviceOtaLog.builder()
                    .deviceUid(deviceUid)
                    .userId(userId)
                    .targetVersion(firmwareInfo.getVersion())
                    .actionType(DeviceOtaLog.ACTION_CHECK)
                    .statusCode(otaStatus.getResult())
                    .apiResponse(otaStatus.getRawResponse())
                    .build();
            deviceOtaLogMapper.insert(otaLog);
            log.debug("OTA检查日志已记录: id={}", otaLog.getId());
        } catch (Exception e) {
            log.error("记录OTA日志失败", e);
        }

        // ============ 步骤 I: 组装返回 DTO ============
        OtaCheckResultDTO dto = OtaCheckResultDTO.builder()
                .deviceUid(deviceUid)
                .currentVersion(currentVersion)
                .latestVersion(firmwareInfo.getVersion())
                .updateDesc(firmwareInfo.getDescription())
                .fileSize(firmwareInfo.getFileSizeFormatted())
                .publishDate(firmwareInfo.getPublishDate())
                .status(status)
                .statusText(statusText)
                .progress(progress)
                .canUpgrade(canUpgrade)
                .hasNewVersion(hasNewVersion)
                .isForce(firmwareInfo.isForceUpdate())
                .build();

        return Result.success(dto);
    }

    /**
     * 执行立即升级
     *
     * @param userId    当前登录用户ID (JWT 解析)
     * @param deviceUid 设备UID
     * @return 操作结果
     */
    @Operation(summary = "执行立即升级", description = "向指定设备下发OTA升级指令。⚠️ 只有设备管理员(is_owner=1)才能操作。需要JWT鉴权")
    @PostMapping("/upgrade")
    public Result<String> triggerUpgrade(
            @RequestAttribute("currentUserId") Long userId,
            @Parameter(description = "设备UID", required = true) @RequestParam String deviceUid) {

        log.info("发起OTA升级指令: userId={}, deviceUid={}", userId, deviceUid);

        // ============ 步骤 A: 校验权限（必须是管理员） ============
        UserDeviceRel userDevice = userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
        if (userDevice == null) {
            log.warn("用户无权访问该设备: userId={}, deviceUid={}", userId, deviceUid);
            return Result.error(403, "无权访问该设备");
        }

        // 🔐 关键鉴权: 只有 is_owner=1 的管理员才能发起升级
        if (userDevice.getIsOwner() == null || userDevice.getIsOwner() != 1) {
            log.warn("普通成员无权发起升级: userId={}, deviceUid={}, isOwner={}",
                    userId, deviceUid, userDevice.getIsOwner());
            return Result.error(403, "只有设备管理员才能发起升级");
        }

        // ============ 步骤 B: 获取设备密钥 ============
        DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (deviceInfo == null) {
            log.error("设备信息不存在: deviceUid={}", deviceUid);
            return Result.error("设备信息异常");
        }
        String secretKey = deviceInfo.getSecretKey();

        // ============ 步骤 C: 发起升级指令 ============
        boolean success;
        String errorMessage = null;
        try {
            success = thirdPartyOtaService.triggerFirmwareUpgrade(deviceUid, secretKey);
        } catch (RuntimeException e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("发起升级失败: deviceUid={}", deviceUid, e);
        }

        // ============ 步骤 D: 记录日志 ============
        try {
            DeviceOtaLog otaLog = DeviceOtaLog.builder()
                    .deviceUid(deviceUid)
                    .userId(userId)
                    .actionType(DeviceOtaLog.ACTION_UPGRADE)
                    .statusCode(success ? 1 : 0)
                    .apiResponse(success ? "升级指令下发成功" : errorMessage)
                    .build();
            deviceOtaLogMapper.insert(otaLog);
            log.debug("OTA升级日志已记录: id={}", otaLog.getId());
        } catch (Exception e) {
            log.error("记录OTA日志失败", e);
        }

        // ============ 步骤 E: 返回结果 ============
        if (success) {
            log.info("升级指令下发成功: deviceUid={}", deviceUid);
            return Result.success("指令已下发");
        } else {
            return Result.error(errorMessage != null ? errorMessage : "升级指令下发失败");
        }
    }
}
