package com.aiqutepets.service.impl;

import com.aiqutepets.config.ThirdPartyConfig;
import com.aiqutepets.dto.FirmwareCheckResponse;
import com.aiqutepets.service.ThirdPartyOtaService;
import com.aiqutepets.vo.AiGrowthStatsVO;
import com.aiqutepets.vo.AiMoodVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.entity.DeviceOtaLog;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.mapper.DeviceOtaLogMapper;

/**
 * 第三方 OTA 固件升级服务实现类
 */
@Slf4j
@Service
public class ThirdPartyOtaServiceImpl implements ThirdPartyOtaService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThirdPartyConfig thirdPartyConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DeviceOtaLogMapper deviceOtaLogMapper;

    /**
     * 更新状态常量
     */
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_DOWNLOAD_COMPLETE = 2;
    private static final int STATUS_DOWNLOAD_FAILED = 3;
    private static final int STATUS_UPGRADING = 4;
    private static final int STATUS_UPGRADE_SUCCESS = 5;
    private static final int STATUS_UPGRADE_FAILED = 6;

    /**
     * 升级超时阈值（分钟）
     */
    private static final long UPGRADE_TIMEOUT_MINUTES = 60;

    @Override
    public FirmwareCheckResponse checkFirmwareUpdate(String deviceUid, String secretKey) {
        log.info("检查设备固件更新状态: deviceUid={}", deviceUid);

        try {
            // 1. 获取全局签名密钥（用于计算签名）
            String signSecret = thirdPartyConfig.getSignSecret();
            if (signSecret == null || signSecret.isEmpty()) {
                throw new RuntimeException("配置错误: sign-secret 未在 application.yml 中配置");
            }

            // 2. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("uid", deviceUid);
            params.put("timestamp", String.valueOf(timestamp));

            // 3. 使用全局密钥生成签名（密钥仅用于计算，不传输）
            String signature = generateSignature(params, signSecret);
            params.put("signature", signature);

            // 4. 构造请求 URL
            String url = buildRequestUrl(params);
            log.debug("请求第三方 OTA 接口: {}", url);

            // 5. 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // 6. 解析响应
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("检查固件更新状态失败: deviceUid={}", deviceUid, e);
            return FirmwareCheckResponse.builder()
                    .hasNewVersion(false)
                    .updateStatus(STATUS_IDLE)
                    .statusDesc("检查更新失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 生成签名
     * 签名规则（与ThirdPartyClient保持一致）：
     * 1. 参数名按字母排序
     * 2. 拼接 key=value&key=value
     * 3. 末尾加上 &secretkey=xxx
     * 4. 转小写后 MD5
     *
     * @param params    请求参数
     * @param secretKey 设备密钥
     * @return 签名字符串（32位小写）
     */
    private String generateSignature(Map<String, String> params, String secretKey) {
        // TreeMap 已经按 key 排序
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        // 与 ThirdPartyClient 保持一致：末尾追加 &secretkey=xxx
        sb.append("&secretkey=").append(secretKey);

        // 转小写后 MD5（与 ThirdPartyClient 保持一致）
        String signStr = sb.toString().toLowerCase();
        log.debug("签名原文: {}", signStr);

        return md5(signStr);
    }

    /**
     * MD5 加密
     *
     * @param input 输入字符串
     * @return MD5 哈希值（32位小写）
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    /**
     * 构造请求 URL
     *
     * @param params 请求参数
     * @return 完整的请求 URL
     */
    private String buildRequestUrl(Map<String, String> params) {
        String baseUrl = thirdPartyConfig.getBaseUrl();
        String otaPath = thirdPartyConfig.getOtaStatusUrl();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + otaPath);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        return builder.toUriString();
    }

    /**
     * 解析第三方接口响应
     * 
     * 响应格式:
     * {
     * "result": "1", // 1=成功
     * "isUpdate": 1, // 1=可以升级, 0=不需要升级
     * "version": "xxx", // 设备当前版本号
     * "updateVersion": "xxx" // 目标版本号
     * }
     *
     * @param responseBody 响应体
     * @return 固件检查结果
     */
    private FirmwareCheckResponse parseResponse(String responseBody) {
        log.debug("第三方 OTA 接口响应: {}", responseBody);

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // result 可能是字符串 "1" 或数字 1
            String resultStr = root.path("result").asText("0");
            int result = "1".equals(resultStr) ? 1 : root.path("result").asInt(0);

            // isUpdate: 1=可以升级, 0=不需要升级
            int isUpdate = root.path("isUpdate").asInt(0);

            // version: 设备当前版本号
            String currentVersion = root.path("version").asText("");

            // updateVersion: 目标版本号
            String latestVersion = root.path("updateVersion").asText("");

            // 兼容旧字段名
            if (latestVersion.isEmpty()) {
                latestVersion = root.path("latestVersion").asText("");
            }

            // 更新状态（此接口主要返回是否有更新，没有详细的下载/升级状态）
            int updateStatus = STATUS_IDLE;

            // 判断是否有新版本：result=1 且 isUpdate=1
            boolean hasNewVersion = (result == 1) && (isUpdate == 1);

            // 生成状态描述
            String statusDesc;
            if (result != 1) {
                statusDesc = "查询失败";
            } else if (hasNewVersion) {
                statusDesc = "有新版本可更新";
            } else {
                statusDesc = "已是最新版本";
            }

            return FirmwareCheckResponse.builder()
                    .hasNewVersion(hasNewVersion)
                    .currentVersion(currentVersion)
                    .latestVersion(latestVersion)
                    .updateStatus(updateStatus)
                    .statusDesc(statusDesc)
                    .progress(0)
                    .updateDescription("")
                    .build();

        } catch (Exception e) {
            log.error("解析 OTA 响应失败", e);
            return FirmwareCheckResponse.builder()
                    .hasNewVersion(false)
                    .updateStatus(STATUS_IDLE)
                    .statusDesc("解析响应失败")
                    .build();
        }
    }

    /**
     * 获取状态描述
     *
     * @param updateStatus 状态码
     * @return 状态描述
     */
    private String getStatusDescription(int updateStatus) {
        switch (updateStatus) {
            case STATUS_IDLE:
                return "空闲";
            case STATUS_DOWNLOADING:
                return "固件下载中";
            case STATUS_DOWNLOAD_COMPLETE:
                return "下载完成";
            case STATUS_DOWNLOAD_FAILED:
                return "下载失败";
            case STATUS_UPGRADING:
                return "升级中";
            case STATUS_UPGRADE_SUCCESS:
                return "升级成功";
            case STATUS_UPGRADE_FAILED:
                return "升级失败";
            default:
                return "未知状态";
        }
    }

    @Override
    public AiGrowthStatsVO getDeviceGrowthStats(String deviceUid, String secretKey) {
        log.info("获取设备成长统计: deviceUid={}", deviceUid);

        try {
            // 1. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("timestamp", String.valueOf(timestamp));

            // 2. 生成签名
            String signature = generateSignature(params, secretKey);
            params.put("signature", signature);

            // 3. 构造请求 URL (替换路径变量)
            String baseUrl = thirdPartyConfig.getBaseUrl();
            String growthPath = "/api/devices/" + deviceUid + "/stats/growth";

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + growthPath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String url = builder.toUriString();
            log.debug("请求成长统计接口: {}", url);

            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // 5. 解析响应
            AiGrowthStatsVO result = objectMapper.readValue(response.getBody(), AiGrowthStatsVO.class);
            log.debug("成长统计响应: {}", result);

            return result;

        } catch (Exception e) {
            log.error("获取设备成长统计失败: deviceUid={}", deviceUid, e);
            // 返回空对象，避免 NPE
            AiGrowthStatsVO errorResponse = new AiGrowthStatsVO();
            errorResponse.setStatus("error");
            return errorResponse;
        }
    }

    @Override
    public int calculateIntimacyPercentage(AiGrowthStatsVO stats) {
        if (stats == null || stats.getData() == null) {
            return 0;
        }

        AiGrowthStatsVO.LevelValues currentValues = stats.getData().getCurrentLevelValues();
        AiGrowthStatsVO.LevelValues nextRequirements = stats.getData().getNextLevelRequirements();

        if (currentValues == null || nextRequirements == null) {
            return 0;
        }

        Integer currentIntimacy = currentValues.getIntimacyValue();
        Integer nextIntimacy = nextRequirements.getIntimacyValue();

        // 防止除以零
        if (nextIntimacy == null || nextIntimacy == 0) {
            return 0;
        }

        if (currentIntimacy == null) {
            return 0;
        }

        // 计算百分比，取整数 (0-100)
        int percentage = (int) ((currentIntimacy * 100.0) / nextIntimacy);

        // 限制范围在 0-100
        return Math.min(100, Math.max(0, percentage));
    }

    /**
     * 默认心情兜底文案
     */
    private static final String DEFAULT_MOOD_CONTENT = "今天也是充满活力的一天，期待和你说话哦！";

    @Override
    public String getDeviceTodayMood(String deviceUid, String secretKey) {
        log.info("获取设备今日心情: deviceUid={}", deviceUid);

        try {
            // 1. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("timestamp", String.valueOf(timestamp));

            // 2. 生成签名
            String signature = generateSignature(params, secretKey);
            params.put("signature", signature);

            // 3. 构造请求 URL
            String baseUrl = thirdPartyConfig.getBaseUrl();
            String moodPath = "/api/devices/" + deviceUid + "/mood/today";

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + moodPath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String url = builder.toUriString();
            log.debug("请求今日心情接口: {}", url);

            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // 5. 解析响应
            AiMoodVO result = objectMapper.readValue(response.getBody(), AiMoodVO.class);
            log.debug("今日心情响应: {}", result);

            // 6. 提取 mood_content
            if (result != null && "success".equals(result.getStatus()) && result.getData() != null) {
                String moodContent = result.getData().getMoodContent();
                if (moodContent != null && !moodContent.isEmpty()) {
                    return moodContent;
                }
            }

            // 响应无效，返回兜底文案
            log.warn("AI 心情接口返回数据无效，使用兜底文案: deviceUid={}", deviceUid);
            return DEFAULT_MOOD_CONTENT;

        } catch (Exception e) {
            log.error("获取设备今日心情失败，使用兜底文案: deviceUid={}", deviceUid, e);
            return DEFAULT_MOOD_CONTENT;
        }
    }

    @Override
    public com.aiqutepets.vo.OtaStatusVO getOtaStatus(String deviceUid, String secretKey) {
        log.info("查询设备OTA升级状态: deviceUid={}", deviceUid);

        try {
            // 1. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("uid", deviceUid);
            params.put("timestamp", String.valueOf(timestamp));

            // 2. 生成签名
            String signature = generateSignature(params, secretKey);
            params.put("signature", signature);

            // 3. 构造请求 URL
            String baseUrl = thirdPartyConfig.getBaseUrl();
            String otaPath = thirdPartyConfig.getOtaGetUpdateStatusUrl();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + otaPath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String url = builder.toUriString();
            log.debug("请求OTA升级状态接口: {}", url);

            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.debug("OTA升级状态接口响应: {}", responseBody);

            // 5. 解析响应
            com.aiqutepets.vo.OtaStatusVO statusVO = parseOtaStatusResponse(deviceUid, responseBody);

            // 6. 升级超时检测 (Timeout Guard)
            return checkUpgradeTimeout(statusVO, deviceUid);

        } catch (Exception e) {
            log.error("查询设备OTA升级状态失败: deviceUid={}", deviceUid, e);
            return com.aiqutepets.vo.OtaStatusVO.builder()
                    .deviceUid(deviceUid)
                    .result(0)
                    .status(STATUS_IDLE)
                    .statusText("查询失败: " + e.getMessage())
                    .hasNewVersion(false)
                    .build();
        }
    }

    /**
     * 升级超时检测 (Timeout Guard)
     *
     * <p>
     * 如果状态为 UPGRADING(4) 且超过 60 分钟，强制返回 FAIL(6) 状态
     * </p>
     *
     * @param statusVO  原始状态 VO
     * @param deviceUid 设备UID
     * @return 处理后的状态 VO
     */
    private com.aiqutepets.vo.OtaStatusVO checkUpgradeTimeout(
            com.aiqutepets.vo.OtaStatusVO statusVO, String deviceUid) {

        // 只检查升级中(4) 状态
        if (statusVO.getStatus() == null || statusVO.getStatus() != STATUS_UPGRADING) {
            return statusVO;
        }

        // 查询最后一次升级操作时间
        DeviceOtaLog latestLog = deviceOtaLogMapper.selectLatestByDeviceUidAndAction(
                deviceUid, DeviceOtaLog.ACTION_UPGRADE);

        if (latestLog == null || latestLog.getCreateTime() == null) {
            return statusVO;
        }

        // 计算时间差
        Duration duration = Duration.between(latestLog.getCreateTime(), LocalDateTime.now());
        long minutesPassed = duration.toMinutes();

        // 超过 60 分钟，强制返回失败状态
        if (minutesPassed > UPGRADE_TIMEOUT_MINUTES) {
            log.warn("OTA升级超时: deviceUid={}, 已过 {} 分钟", deviceUid, minutesPassed);
            return com.aiqutepets.vo.OtaStatusVO.builder()
                    .deviceUid(deviceUid)
                    .result(statusVO.getResult())
                    .status(STATUS_UPGRADE_FAILED) // 强制设为 6
                    .statusText("升级响应超时，请重启设备后重试")
                    .hasNewVersion(false)
                    .currentVersion(statusVO.getCurrentVersion())
                    .targetVersion(statusVO.getTargetVersion())
                    .progress(statusVO.getProgress())
                    .rawResponse(statusVO.getRawResponse())
                    .build();
        }

        return statusVO;
    }

    /**
     * 解析 OTA 升级状态响应
     *
     * @param deviceUid    设备UID
     * @param responseBody 响应体
     * @return OtaStatusVO
     */
    private com.aiqutepets.vo.OtaStatusVO parseOtaStatusResponse(String deviceUid, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            int result = root.path("result").asInt(0);
            int status = root.path("status").asInt(STATUS_IDLE);
            String currentVersion = root.path("currentVersion").asText(
                    root.path("current_version").asText(""));
            String targetVersion = root.path("targetVersion").asText(
                    root.path("target_version").asText(
                            root.path("latestVersion").asText("")));
            int progress = root.path("progress").asInt(0);
            String updateDescription = root.path("updateDescription").asText(
                    root.path("update_description").asText(""));

            // 判断是否有新版本
            // result=1 且 status 不为升级成功(5) 且有目标版本号
            boolean hasNewVersion = (result == 1)
                    && (status != STATUS_UPGRADE_SUCCESS)
                    && (targetVersion != null && !targetVersion.isEmpty());

            // 生成状态描述
            String statusDesc = getStatusDescription(status);
            if (status == STATUS_DOWNLOADING || status == STATUS_UPGRADING) {
                statusDesc = statusDesc + " (" + progress + "%)";
            }

            return com.aiqutepets.vo.OtaStatusVO.builder()
                    .deviceUid(deviceUid)
                    .result(result)
                    .status(status)
                    .statusText(statusDesc)
                    .hasNewVersion(hasNewVersion)
                    .currentVersion(currentVersion)
                    .targetVersion(targetVersion)
                    .progress(progress)
                    .updateDescription(updateDescription)
                    .rawResponse(responseBody)
                    .build();

        } catch (Exception e) {
            log.error("解析OTA升级状态响应失败", e);
            return com.aiqutepets.vo.OtaStatusVO.builder()
                    .deviceUid(deviceUid)
                    .result(0)
                    .status(STATUS_IDLE)
                    .statusText("解析响应失败")
                    .hasNewVersion(false)
                    .rawResponse(responseBody)
                    .build();
        }
    }

    @Override
    public boolean triggerFirmwareUpgrade(String deviceUid, String ignoredSecretKey) {
        log.info("发起设备固件升级指令: deviceUid={}", deviceUid);

        try {
            // 1. 获取全局签名密钥（用于计算签名，不用于传输！）
            String signSecret = thirdPartyConfig.getSignSecret();
            if (signSecret == null || signSecret.isEmpty()) {
                throw new RuntimeException("配置错误: sign-secret 未在 application.yml 中配置");
            }

            // 2. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("uid", deviceUid);
            params.put("timestamp", String.valueOf(timestamp));

            // 3. 使用全局密钥生成签名（密钥仅用于计算，绝不传输）
            String signature = generateSignature(params, signSecret);
            params.put("signature", signature);

            // 4. 构造请求 URL（GET 方式，参数通过 Query String 传递）
            String baseUrl = thirdPartyConfig.getBaseUrl();
            String upgradePath = thirdPartyConfig.getOtaUpgradeDeviceUrl();

            // 构建带参数的 URL
            org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(baseUrl + upgradePath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String url = builder.toUriString();
            log.debug("发起固件升级请求(GET): url={}", url);

            // 5. 发送 GET 请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.debug("固件升级接口响应: {}", responseBody);

            // 5. 解析响应
            JsonNode root = objectMapper.readTree(responseBody);
            int result = root.path("result").asInt(0);

            if (result == 1) {
                log.info("固件升级指令发送成功: deviceUid={}", deviceUid);
                return true;
            } else {
                String message = root.path("message").asText(
                        root.path("msg").asText("升级指令发送失败"));
                log.warn("固件升级指令发送失败: deviceUid={}, result={}, message={}",
                        deviceUid, result, message);
                throw new RuntimeException("发起升级失败: " + message);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("发起设备固件升级指令失败: deviceUid={}", deviceUid, e);
            throw new RuntimeException("发起升级失败: " + e.getMessage(), e);
        }
    }

    @Override
    public com.aiqutepets.vo.OtaFirmwareInfoVO getLatestFirmwareInfo(String deviceUid, String secretKey,
            String currentVersion) {
        log.info("查询设备最新固件信息: deviceUid={}, currentVersion={}", deviceUid, currentVersion);

        try {
            // 1. 构造请求参数
            long timestamp = System.currentTimeMillis();
            Map<String, String> params = new TreeMap<>();
            params.put("uid", deviceUid);
            params.put("timestamp", String.valueOf(timestamp));

            // 如果传入了当前版本号，添加到参数中（用于服务端对比）
            if (currentVersion != null && !currentVersion.isEmpty()) {
                params.put("version", currentVersion);
            }

            // 1.5 获取设备型号并传递 deviceType 参数 (Compatibility)
            DeviceInfo deviceInfo = deviceInfoMapper.selectByDeviceUid(deviceUid);
            if (deviceInfo != null && deviceInfo.getProductModel() != null
                    && !deviceInfo.getProductModel().isEmpty()) {
                params.put("deviceType", deviceInfo.getProductModel());
                log.debug("添加 deviceType 参数: {}", deviceInfo.getProductModel());
            }

            // 2. 生成签名
            String signature = generateSignature(params, secretKey);
            params.put("signature", signature);

            // 3. 构造请求 URL
            String baseUrl = thirdPartyConfig.getBaseUrl();
            String firmwarePath = thirdPartyConfig.getOtaGetLatestFirmwareUrl();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + firmwarePath);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            String url = builder.toUriString();
            log.debug("请求最新固件信息接口: {}", url);

            // 4. 发送请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            log.debug("最新固件信息接口响应: {}", responseBody);

            // 5. 解析响应
            return parseFirmwareInfoResponse(responseBody);

        } catch (Exception e) {
            log.error("查询设备最新固件信息失败: deviceUid={}", deviceUid, e);
            // 返回"无更新"的默认对象，避免阻断主流程
            return com.aiqutepets.vo.OtaFirmwareInfoVO.noUpdate();
        }
    }

    /**
     * 解析最新固件信息响应
     *
     * @param responseBody 响应体
     * @return OtaFirmwareInfoVO
     */
    private com.aiqutepets.vo.OtaFirmwareInfoVO parseFirmwareInfoResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            int result = root.path("result").asInt(0);

            // 如果 result 不为 1，返回"无更新"
            if (result != 1) {
                log.warn("获取固件信息接口返回失败: result={}", result);
                return com.aiqutepets.vo.OtaFirmwareInfoVO.builder()
                        .result(result)
                        .isUpdate(0)
                        .rawResponse(responseBody)
                        .build();
            }

            // 解析各字段，兼容多种字段命名
            int isUpdate = root.path("isUpdate").asInt(
                    root.path("is_update").asInt(0));
            String version = root.path("version").asText(
                    root.path("latestVersion").asText(""));
            String description = root.path("description").asText(
                    root.path("updateDescription").asText(
                            root.path("desc").asText("")));
            String publishDate = root.path("publishDate").asText(
                    root.path("publish_date").asText(""));
            long fileSize = root.path("fileSize").asLong(
                    root.path("file_size").asLong(0));
            int isForce = root.path("isForce").asInt(
                    root.path("is_force").asInt(0));

            return com.aiqutepets.vo.OtaFirmwareInfoVO.builder()
                    .result(result)
                    .isUpdate(isUpdate)
                    .version(version)
                    .description(description)
                    .publishDate(publishDate)
                    .fileSize(fileSize)
                    .isForce(isForce)
                    .rawResponse(responseBody)
                    .build();

        } catch (Exception e) {
            log.error("解析固件信息响应失败", e);
            return com.aiqutepets.vo.OtaFirmwareInfoVO.builder()
                    .result(0)
                    .isUpdate(0)
                    .rawResponse(responseBody)
                    .build();
        }
    }
}
