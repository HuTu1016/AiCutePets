package com.aiqutepets.util;

import com.aiqutepets.config.ThirdPartyConfig;
import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.vo.AiBadgeListVO;
import com.aiqutepets.vo.AiDiaryDateVO;
import com.aiqutepets.vo.AiDiaryDetailVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 第三方接口客户端工具类
 * 
 * <p>
 * 用于对接第三方 HTTP 接口，包含签名算法和通用请求方法。
 * </p>
 * 
 * <h3>签名规则（参考 AI 玩具接入文档 1.4 节）：</h3>
 * <ol>
 * <li>对请求参数（除 signature）按 key 升序排序</li>
 * <li>拼接成 key1=value1&key2=value2 格式</li>
 * <li>末尾追加 &secretkey=xxx</li>
 * <li>将整个字符串转小写后进行 MD5 加密（32位小写）</li>
 * </ol>
 * 
 * <p>
 * <b>注意：SecretKey 从数据库 device_info 表获取，绝不暴露给前端！</b>
 * </p>
 */
@Slf4j
@Component
public class ThirdPartyClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ThirdPartyConfig thirdPartyConfig;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    /**
     * 生成签名
     * 
     * <p>
     * 签名规则：
     * </p>
     * <ol>
     * <li>对参数按 key 升序排序</li>
     * <li>拼接成 key=value&key=value 格式</li>
     * <li>末尾加 &secretkey=xxx</li>
     * <li>转小写后 MD5 加密（32位小写）</li>
     * </ol>
     *
     * @param params    请求参数（不含 signature）
     * @param secretKey 设备密钥
     * @return MD5 签名字符串（32位小写）
     */
    public String generateSignature(Map<String, Object> params, String secretKey) {
        // 1. 过滤掉 signature 参数（如果存在）
        Map<String, Object> filteredParams = new TreeMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!"signature".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                filteredParams.put(entry.getKey(), entry.getValue());
            }
        }

        // 2. 按 key 升序排序并拼接（TreeMap 自动排序）
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : filteredParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        // 3. 末尾追加 secretkey
        sb.append("&secretkey=").append(secretKey);

        // 4. 转小写后 MD5 加密
        String stringToSign = sb.toString().toLowerCase();
        log.debug("待签名字符串: {}", stringToSign);

        return md5(stringToSign);
    }

    /**
     * MD5 加密
     *
     * @param input 输入字符串
     * @return 32位小写 MD5 值
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
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
     * 发送带签名的 GET 请求
     * 
     * <p>
     * 自动添加 timestamp 和 signature 参数
     * </p>
     *
     * @param url       请求 URL（不含 query 参数）
     * @param params    请求参数
     * @param secretKey 设备密钥
     * @return 响应结果
     */
    public String sendGetRequest(String url, Map<String, Object> params, String secretKey) {
        // 1. 添加 timestamp
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("timestamp", System.currentTimeMillis());

        // 2. 生成签名
        String signature = generateSignature(allParams, secretKey);
        allParams.put("signature", signature);

        // 3. 构建 URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (Map.Entry<String, Object> entry : allParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        String fullUrl = builder.toUriString();

        log.info("发送第三方请求: {}", fullUrl);

        // 4. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
            log.info("第三方接口响应: status={}", response.getStatusCode());
            // 处理响应（含解密逻辑）
            return processResponse(response, secretKey);
        } catch (Exception e) {
            log.error("第三方接口请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("第三方接口请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送带签名的 POST 请求
     *
     * @param url       请求 URL
     * @param params    请求参数
     * @param secretKey 设备密钥
     * @return 响应结果
     */
    public String sendPostRequest(String url, Map<String, Object> params, String secretKey) {
        // 1. 添加 timestamp
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("timestamp", System.currentTimeMillis());

        // 2. 生成签名
        String signature = generateSignature(allParams, secretKey);
        allParams.put("signature", signature);

        log.info("发送第三方 POST 请求: url={}", url);

        // 3. 发送请求
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(allParams, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("第三方接口响应: status={}", response.getStatusCode());
            // 处理响应（含解密逻辑）
            return processResponse(response, secretKey);
        } catch (Exception e) {
            log.error("第三方接口请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("第三方接口请求失败: " + e.getMessage(), e);
        }
    }

    // ==================== 报文解密工具方法 ====================

    /**
     * 处理响应内容（包含解密逻辑）
     *
     * <p>
     * 解密触发条件（满足其一）：
     * <ol>
     * <li>响应头包含 X-Encryption: true</li>
     * <li>JSON 解析失败</li>
     * </ol>
     * </p>
     *
     * @param response  HTTP 响应
     * @param secretKey 设备密钥（用于解密）
     * @return 处理后的响应内容
     */
    private String processResponse(ResponseEntity<String> response, String secretKey) {
        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            return body;
        }

        boolean needDecrypt = false;

        // 方式1: 检查响应头标记
        HttpHeaders headers = response.getHeaders();
        List<String> encryptionHeader = headers.get("X-Encryption");
        if (encryptionHeader != null && encryptionHeader.contains("true")) {
            needDecrypt = true;
            log.debug("检测到加密响应头 X-Encryption: true");
        }

        // 方式2: 尝试解析 JSON，如果失败则尝试解密
        if (!needDecrypt) {
            try {
                objectMapper.readTree(body);
            } catch (Exception e) {
                log.debug("JSON 解析失败，尝试解密响应内容");
                needDecrypt = true;
            }
        }

        // 执行解密
        if (needDecrypt && secretKey != null && !secretKey.isEmpty()) {
            try {
                body = aesDecrypt(body, secretKey);
                log.debug("报文解密成功");
            } catch (Exception e) {
                log.warn("报文解密失败，返回原始内容: {}", e.getMessage());
            }
        }

        return body;
    }

    /**
     * AES 解密（ECB 模式）
     * 
     * <p>
     * 根据《AI 玩具接入文档》1.3.1 节：
     * <ul>
     * <li>算法: AES/ECB/PKCS5Padding</li>
     * <li>密钥: 固定密钥 "885ee6378f2b29c8" (16位，AES-128)</li>
     * </ul>
     * </p>
     * 
     * <p>
     * 解密流程:
     * <ol>
     * <li>获取响应的 Base64 字符串</li>
     * <li>Base64 解码 -> 得到字节数组 byte[]</li>
     * <li>AES 解密 (ECB模式, 无IV) -> 得到解密后的 byte[]</li>
     * <li>转 UTF-8 字符串 -> 解析 JSON</li>
     * </ol>
     * </p>
     *
     * @param encryptedData Base64 编码的加密数据
     * @param secretKey     此参数暂不使用，保留以兼容接口（实际使用固定密钥）
     * @return 解密后的明文
     */
    private String aesDecrypt(String encryptedData, String secretKey) {
        // 固定解密密钥（参考《AI 玩具接入文档》1.3.1 节）
        final String DECRYPT_KEY = "885ee6378f2b29c8";

        try {
            // 1. 创建 AES/ECB/PKCS5Padding 解密器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            // 2. 使用固定密钥（16位，AES-128）
            byte[] keyBytes = DECRYPT_KEY.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // 3. Base64 解码 -> AES 解密
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 4. 转 UTF-8 字符串
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("AES 解密成功，明文长度: {} 字符", result.length());
            return result;

        } catch (Exception e) {
            log.error("AES 解密失败: {}", e.getMessage());
            throw new RuntimeException("报文解密失败", e);
        }
    }

    // ==================== 具体业务方法 ====================

    /**
     * 从数据库获取设备密钥
     *
     * @param deviceUid 设备 UID
     * @return 设备密钥
     */
    private String getSecretKey(String deviceUid) {
        DeviceInfo device = deviceInfoMapper.selectByDeviceUid(deviceUid);
        if (device == null) {
            throw new RuntimeException("设备不存在: " + deviceUid);
        }
        return device.getSecretKey();
    }

    /**
     * 获取设备日记列表
     *
     * @param deviceUid 设备 UID
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate   结束日期（格式：yyyy-MM-dd）
     * @return 日记列表 JSON 字符串
     */
    public String getDiaryList(String deviceUid, String startDate, String endDate) {
        String secretKey = getSecretKey(deviceUid);

        Map<String, Object> params = new HashMap<>();
        params.put("uid", deviceUid);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        String url = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getDiaryListUrl();
        return sendGetRequest(url, params, secretKey);
    }

    /**
     * 获取设备日记列表（指定密钥版本，用于测试或特殊场景）
     *
     * @param deviceUid 设备 UID
     * @param secretKey 设备密钥
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 日记列表 JSON 字符串
     */
    public String getDiaryList(String deviceUid, String secretKey, String startDate, String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", deviceUid);
        params.put("startDate", startDate);
        params.put("endDate", endDate);

        String url = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getDiaryListUrl();
        return sendGetRequest(url, params, secretKey);
    }

    /**
     * 检查设备 OTA 状态
     *
     * @param deviceUid 设备 UID
     * @return OTA 状态 JSON 字符串
     */
    public String checkOtaStatus(String deviceUid) {
        String secretKey = getSecretKey(deviceUid);

        Map<String, Object> params = new HashMap<>();
        params.put("uid", deviceUid);

        String url = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getOtaStatusUrl();
        return sendGetRequest(url, params, secretKey);
    }

    /**
     * 检查设备 OTA 状态（指定密钥版本）
     *
     * @param deviceUid 设备 UID
     * @param secretKey 设备密钥
     * @return OTA 状态 JSON 字符串
     */
    public String checkOtaStatus(String deviceUid, String secretKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("uid", deviceUid);

        String url = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getOtaStatusUrl();
        return sendGetRequest(url, params, secretKey);
    }

    // ==================== 记忆页面相关方法 ====================

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取日记日期列表
     * 
     * <p>
     * 接口: GET /api/devices/{uid}/diary/dates
     * </p>
     *
     * @param uid       设备 UID
     * @param secretKey 设备密钥
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate   结束日期（格式：yyyy-MM-dd）
     * @return 日记日期列表
     */
    public AiDiaryDateVO[] getDiaryDates(String uid, String secretKey, String startDate, String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        String urlTemplate = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getDiaryDatesUrl();
        String url = urlTemplate.replace("{uid}", uid);

        String responseJson = sendGetRequest(url, params, secretKey);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                return objectMapper.convertValue(dataNode, AiDiaryDateVO[].class);
            }
            // 如果 data 不是数组，尝试直接解析 root
            if (root.isArray()) {
                return objectMapper.convertValue(root, AiDiaryDateVO[].class);
            }
            log.warn("日记日期列表响应格式异常: {}", responseJson);
            return new AiDiaryDateVO[0];
        } catch (Exception e) {
            log.error("解析日记日期列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析日记日期列表失败", e);
        }
    }

    /**
     * 获取日记详情
     * 
     * <p>
     * 接口: GET /api/devices/{uid}/diary/{date}
     * </p>
     *
     * @param uid       设备 UID
     * @param secretKey 设备密钥
     * @param date      日期（格式：yyyy-MM-dd）
     * @return 日记详情
     */
    public AiDiaryDetailVO getDiaryDetail(String uid, String secretKey, String date) {
        Map<String, Object> params = new HashMap<>();

        String urlTemplate = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getDiaryDetailUrl();
        String url = urlTemplate.replace("{uid}", uid).replace("{date}", date);

        String responseJson = sendGetRequest(url, params, secretKey);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode() && dataNode.isObject()) {
                return objectMapper.convertValue(dataNode, AiDiaryDetailVO.class);
            }
            // 如果没有 data 包装，直接解析 root
            return objectMapper.convertValue(root, AiDiaryDetailVO.class);
        } catch (Exception e) {
            log.error("解析日记详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析日记详情失败", e);
        }
    }

    /**
     * 获取徽章列表
     * 
     * <p>
     * 接口: GET /api/devices/{uid}/stats/badges
     * </p>
     *
     * @param uid       设备 UID
     * @param secretKey 设备密钥
     * @return 徽章列表
     */
    public AiBadgeListVO getBadgeList(String uid, String secretKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "all");

        String urlTemplate = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getBadgeListUrl();
        String url = urlTemplate.replace("{uid}", uid);

        String responseJson = sendGetRequest(url, params, secretKey);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode() && dataNode.isObject()) {
                return objectMapper.convertValue(dataNode, AiBadgeListVO.class);
            }
            // 如果没有 data 包装，直接解析 root
            return objectMapper.convertValue(root, AiBadgeListVO.class);
        } catch (Exception e) {
            log.error("解析徽章列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析徽章列表失败", e);
        }
    }

    /**
     * 标记徽章为已展示
     * 
     * <p>
     * 接口: POST /api/devices/{device_uid}/badges/{badge_code}/mark-shown
     * </p>
     *
     * @param deviceUid 设备 UID
     * @param secretKey 设备密钥
     * @param badgeCode 徽章代码
     * @return 是否成功
     */
    public boolean markBadgeAsShown(String deviceUid, String secretKey, String badgeCode) {
        Map<String, Object> params = new HashMap<>();

        String urlTemplate = thirdPartyConfig.getBaseUrl() + thirdPartyConfig.getBadgeMarkShownUrl();
        String url = urlTemplate.replace("{device_uid}", deviceUid).replace("{badge_code}", badgeCode);

        String responseJson = sendPostRequest(url, params, secretKey);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String status = root.path("status").asText();
            boolean success = "success".equalsIgnoreCase(status);
            log.info("标记徽章已展示: deviceUid={}, badgeCode={}, success={}", deviceUid, badgeCode, success);
            return success;
        } catch (Exception e) {
            log.error("标记徽章已展示失败: deviceUid={}, badgeCode={}", deviceUid, badgeCode, e);
            return false;
        }
    }
}
