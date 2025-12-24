package com.aiqutepets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 */
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
}
