package com.aiqutepets.dto;

import lombok.Data;

/**
 * 获取手机号请求 DTO
 */
@Data
public class PhoneRequest {

    /**
     * 手机号授权的动态令牌 code
     */
    private String code;
}
