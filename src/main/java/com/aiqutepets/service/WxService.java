package com.aiqutepets.service;

/**
 * 微信服务接口
 */
public interface WxService {

    /**
     * 获取微信 access_token
     *
     * @return access_token
     */
    String getAccessToken();

    /**
     * 通过 code 获取用户手机号
     *
     * @param code 手机号授权动态令牌
     * @return 纯手机号（不含区号）
     */
    String getPhoneNumber(String code);
}
