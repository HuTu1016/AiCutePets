package com.aiqutepets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方接口配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "thirdparty")
public class ThirdPartyConfig {

    /**
     * 第三方接口基础 URL
     */
    private String baseUrl = "https://api.example.com";

    /**
     * 全局签名密钥（用于 OTA 等接口签名）
     */
    private String signSecret;

    /**
     * AES 解密密钥
     */
    private String aesKey;

    /**
     * 日记列表接口路径
     */
    private String diaryListUrl = "/toy/v2/getDiaryList";

    /**
     * OTA 状态检查接口路径
     */
    private String otaStatusUrl = "/devicemgr/device/CGI!checkOtaStatus.action";

    /**
     * OTA 查询升级状态接口路径（APP端）
     */
    private String otaGetUpdateStatusUrl = "/devicemgr/app/CGI!getDeviceUpdateStatus.action";

    /**
     * OTA 发起升级指令接口路径
     * 
     */
    private String otaUpgradeDeviceUrl = "/devicemgr/app/CGI!setDeviceUpdate.action";

    /**
     * OTA 获取最新固件信息接口路径
     */
    private String otaGetLatestFirmwareUrl = "/devicemgr/app/CGI!getDeviceSoft.action";

    /**
     * 日记日期列表接口路径
     */
    private String diaryDatesUrl = "/api/devices/{uid}/diary/dates";

    /**
     * 日记详情接口路径
     */
    private String diaryDetailUrl = "/api/devices/{uid}/diary/{date}";

    /**
     * 徽章列表接口路径
     */
    private String badgeListUrl = "/api/devices/{uid}/stats/badges";

    /**
     * 徽章标记已展示接口路径
     */
    private String badgeMarkShownUrl = "/api/devices/{device_uid}/badges/{badge_code}/mark-shown";

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 10000;
}
