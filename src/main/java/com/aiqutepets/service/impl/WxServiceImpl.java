package com.aiqutepets.service.impl;

import com.aiqutepets.config.WxConfig;
import com.aiqutepets.dto.WxAccessTokenResponse;
import com.aiqutepets.dto.WxPhoneResponse;
import com.aiqutepets.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信服务实现类
 */
@Slf4j
@Service
public class WxServiceImpl implements WxService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WxConfig wxConfig;

    /**
     * access_token 缓存
     */
    private final Map<String, TokenCache> tokenCache = new ConcurrentHashMap<>();

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String GET_PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    @Override
    public String getAccessToken() {
        // 检查缓存
        TokenCache cache = tokenCache.get("access_token");
        if (cache != null && !cache.isExpired()) {
            log.debug("使用缓存的 access_token");
            return cache.token;
        }

        // 请求新的 access_token
        String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s",
                ACCESS_TOKEN_URL,
                wxConfig.getAppId(),
                wxConfig.getAppSecret());

        log.info("请求微信 access_token");
        WxAccessTokenResponse response = restTemplate.getForObject(url, WxAccessTokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            String errMsg = response != null ? response.getErrmsg() : "响应为空";
            log.error("获取 access_token 失败: {}", errMsg);
            throw new RuntimeException("获取微信 access_token 失败: " + errMsg);
        }

        // 缓存 token（提前5分钟过期）
        long expiresAt = System.currentTimeMillis() + (response.getExpiresIn() - 300) * 1000L;
        tokenCache.put("access_token", new TokenCache(response.getAccessToken(), expiresAt));

        log.info("获取 access_token 成功，有效期 {} 秒", response.getExpiresIn());
        return response.getAccessToken();
    }

    @Override
    public String getPhoneNumber(String code) {
        String accessToken = getAccessToken();

        String url = String.format("%s?access_token=%s", GET_PHONE_URL, accessToken);

        // 构建请求体
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        log.info("请求微信获取手机号接口");
        ResponseEntity<WxPhoneResponse> responseEntity = restTemplate.postForEntity(url, entity, WxPhoneResponse.class);
        WxPhoneResponse response = responseEntity.getBody();

        if (response == null) {
            throw new RuntimeException("获取手机号失败: 响应为空");
        }

        if (response.getErrcode() != null && response.getErrcode() != 0) {
            log.error("获取手机号失败: errcode={}, errmsg={}", response.getErrcode(), response.getErrmsg());
            throw new RuntimeException("获取手机号失败: " + response.getErrmsg());
        }

        if (response.getPhoneInfo() == null) {
            throw new RuntimeException("获取手机号失败: phone_info 为空");
        }

        String phone = response.getPhoneInfo().getPurePhoneNumber();
        log.info("成功获取手机号: {}", phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));

        return phone;
    }

    /**
     * Token 缓存
     */
    private static class TokenCache {
        final String token;
        final long expiresAt;

        TokenCache(String token, long expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
