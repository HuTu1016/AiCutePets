package com.aiqutepets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 第三方接口配置
 * 注意：字段名需与application.yml中的配置一致（驼峰命名 -> kebab-case）
 */
@Data
@Component
@ConfigurationProperties(prefix = "thirdparty")
public class ThirdPartyConfig {

    /**
     * 第三方接口基础 URL
     * 默认值应与 application.yml 配置保持一致
     */
    private String baseUrl = "https://toy.visiondigit.cn";

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
     * 对应 application.yml 中的 ota-status-url
     */
    private String otaStatusUrl = "/devicemgr/device/CGI!checkOtaStatus.action";

    /**
     * OTA 检查版本接口路径（检查是否有新版本）
     * 对应 application.yml 中的 ota-check-ver-url
     */
    private String otaCheckVerUrl = "/devicemgr/app/CGI!getDeviceSoft.action";

    /**
     * OTA 查询升级状态接口路径（APP端）
     */
    private String otaGetUpdateStatusUrl = "/devicemgr/app/CGI!getDeviceUpdateStatus.action";

    /**
     * OTA 发起升级指令接口路径
     * 对应 application.yml 中的 ota-trigger-url
     */
    private String otaUpgradeDeviceUrl = "/devicemgr/app/CGI!setDeviceUpdate.action";
    
    /**
     * 别名：otaTriggerUrl，与 application.yml 保持兼容
     */
    private String otaTriggerUrl = "/devicemgr/app/CGI!setDeviceUpdate.action";

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
