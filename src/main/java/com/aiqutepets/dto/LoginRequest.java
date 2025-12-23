package com.aiqutepets.dto;

import lombok.Data;

/**
 * 微信登录请求 DTO
 */
@Data
public class LoginRequest {

    /**
     * 微信小程序登录 code
     */
    private String code;
}
