package com.aiqutepets.service;

import com.aiqutepets.dto.FirmwareCheckResponse;
import com.aiqutepets.vo.AiGrowthStatsVO;

/**
 * 第三方 OTA 固件升级服务接口
 */
public interface ThirdPartyOtaService {

    /**
     * 检查设备固件更新状态
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return 固件更新状态
     */
    FirmwareCheckResponse checkFirmwareUpdate(String deviceUid, String secretKey);

    /**
     * 获取设备成长统计数据
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return 成长统计 VO
     */
    AiGrowthStatsVO getDeviceGrowthStats(String deviceUid, String secretKey);

    /**
     * 计算亲密度百分比
     *
     * @param stats 成长统计 VO
     * @return 亲密度百分比 (0-100)
     */
    int calculateIntimacyPercentage(AiGrowthStatsVO stats);

    /**
     * 获取设备今日心情
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return 心情内容字符串，调用失败时返回默认兜底文案
     */
    String getDeviceTodayMood(String deviceUid, String secretKey);

    /**
     * 查询设备OTA升级状态
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return OTA升级状态信息
     */
    com.aiqutepets.vo.OtaStatusVO getOtaStatus(String deviceUid, String secretKey);

    /**
     * 发起设备固件升级指令
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return 升级指令是否发送成功
     * @throws RuntimeException 当接口调用失败时抛出异常
     */
    boolean triggerFirmwareUpgrade(String deviceUid, String secretKey);

    /**
     * 获取设备最新固件信息
     *
     * @param deviceUid      设备唯一标识
     * @param secretKey      设备通信密钥
     * @param currentVersion 当前版本号（可选，用于服务端对比）
     * @return 最新固件信息
     */
    com.aiqutepets.vo.OtaFirmwareInfoVO getLatestFirmwareInfo(String deviceUid, String secretKey,
            String currentVersion);

    /**
     * 获取设备最新固件信息（不传当前版本）
     *
     * @param deviceUid 设备唯一标识
     * @param secretKey 设备通信密钥
     * @return 最新固件信息
     */
    default com.aiqutepets.vo.OtaFirmwareInfoVO getLatestFirmwareInfo(String deviceUid, String secretKey) {
        return getLatestFirmwareInfo(deviceUid, secretKey, null);
    }
}
