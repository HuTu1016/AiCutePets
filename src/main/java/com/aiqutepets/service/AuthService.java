package com.aiqutepets.service;

import com.aiqutepets.dto.LoginRequest;
import com.aiqutepets.dto.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 微信小程序登录
     *
     * @param loginRequest 登录请求（包含 code）
     * @return 登录响应（包含 token 和用户信息）
     */
    LoginResponse wxLogin(LoginRequest loginRequest);

    /**
     * 绑定手机号
     *
     * @param userId 当前用户ID
     * @param code   手机号授权动态令牌
     * @return 绑定的手机号
     */
    String bindPhone(Long userId, String code);
}
