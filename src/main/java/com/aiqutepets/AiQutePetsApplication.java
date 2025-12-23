package com.aiqutepets;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI 可爱宠物小程序后端启动类
 */
@SpringBootApplication
@MapperScan("com.aiqutepets.mapper")
@EnableConfigurationProperties
public class AiQutePetsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiQutePetsApplication.class, args);
    }
}
