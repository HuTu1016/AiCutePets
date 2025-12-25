package com.aiqutepets.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 微信小程序配置
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxConfig {

    /**
     * 小程序 appId
     */
    private String appId;

    /**
     * 小程序 appSecret
     */
    private String appSecret;

    /**
     * 微信登录接口地址
     */
    private String jscode2sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

    /**
     * 是否启用 Mock 模式（开发环境使用，生产环境务必设为 false）
     */
    private boolean mockEnabled = false;

    /**
     * Mock 模式下使用的测试 openid
     */
    private String mockOpenid = "test_openid_default";

    /**
     * 当前激活的 Spring Profile
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 启动时检查 Mock 模式安全性
     */
    @PostConstruct
    public void validateMockMode() {
        // 生产环境强制禁用 Mock 模式
        if (isProductionEnvironment() && mockEnabled) {
            log.error("⚠️ 严重安全警告: 生产环境检测到 Mock 模式已启用，已强制禁用!");
            this.mockEnabled = false;
        }
        
        if (mockEnabled) {
            log.warn("⚠️ [开发模式] Mock 登录已启用，生产环境请将 wx.miniapp.mock-enabled 设为 false");
        }
    }

    /**
     * 判断是否为生产环境
     */
    public boolean isProductionEnvironment() {
        return "prod".equalsIgnoreCase(activeProfile) 
                || "production".equalsIgnoreCase(activeProfile)
                || "prd".equalsIgnoreCase(activeProfile);
    }

    /**
     * 安全获取 Mock 状态（生产环境始终返回 false）
     */
    public boolean isMockEnabled() {
        if (isProductionEnvironment()) {
            return false;
        }
        return mockEnabled;
    }
}
