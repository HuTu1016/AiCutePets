package com.aiqutepets.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 密钥
     */
    private String secret = "aiqutepets-jwt-secret-key-2025";

    /**
     * JWT 过期时间（毫秒），默认7天
     */
    private Long expiration = 7 * 24 * 60 * 60 * 1000L;
}
