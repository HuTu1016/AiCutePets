package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.LoginRequest;
import com.aiqutepets.dto.LoginResponse;
import com.aiqutepets.dto.PhoneRequest;
import com.aiqutepets.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口", description = "微信小程序登录、手机号绑定等认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 微信小程序登录
     *
     * @param loginRequest 登录请求（包含 code）
     * @return 登录响应（包含 token 和用户信息）
     */
    @Operation(summary = "微信登录", description = "使用微信小程序 code 换取 JWT Token，无需鉴权")
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("收到微信登录请求");

        if (loginRequest.getCode() == null || loginRequest.getCode().isEmpty()) {
            return Result.error(400, "code 不能为空");
        }

        try {
            LoginResponse response = authService.wxLogin(loginRequest);
            return Result.success(response);
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取手机号并绑定
     * 需要 JWT 鉴权
     *
     * @param phoneRequest 手机号授权请求（包含 code）
     * @param userId       当前用户ID（由 JwtInterceptor 设置）
     * @return 绑定的手机号
     */
    @Operation(summary = "绑定手机号", description = "使用微信手机号授权 code 获取并绑定手机号，需要 JWT 鉴权")
    @PostMapping("/phone")
    public Result<String> bindPhone(
            @RequestBody PhoneRequest phoneRequest,
            @RequestAttribute("currentUserId") Long userId) {
        log.info("收到获取手机号请求");

        if (phoneRequest.getCode() == null || phoneRequest.getCode().isEmpty()) {
            return Result.error(400, "code 不能为空");
        }

        try {
            String phone = authService.bindPhone(userId, phoneRequest.getCode());
            return Result.success(phone);
        } catch (Exception e) {
            log.error("获取手机号失败", e);
            return Result.error("获取手机号失败: " + e.getMessage());
        }
    }
}
