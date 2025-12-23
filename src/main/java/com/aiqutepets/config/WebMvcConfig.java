package com.aiqutepets.config;

import com.aiqutepets.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 需要鉴权的接口路径
                .addPathPatterns("/**")
                // 排除不需要鉴权的接口
                .excludePathPatterns(
                        "/api/auth/login", // 登录接口不需要鉴权
                        "/api/device/check-valid", // 设备校验接口不需要鉴权
                        "/api/content/**", // 内容接口不需要鉴权（帮助文档等）
                        // Knife4j / Swagger 相关路径
                        "/doc.html",
                        "/doc.html/**",
                        "/webjars/**",
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/favicon.ico",
                        "/error");
    }
}
