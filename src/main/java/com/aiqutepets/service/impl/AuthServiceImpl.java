package com.aiqutepets.service.impl;

import com.aiqutepets.config.WxConfig;
import com.aiqutepets.dto.LoginRequest;
import com.aiqutepets.dto.LoginResponse;
import com.aiqutepets.dto.WxSessionResponse;
import com.aiqutepets.entity.MpUser;
import com.aiqutepets.mapper.MpUserMapper;
import com.aiqutepets.service.AuthService;
import com.aiqutepets.service.WxService;
import com.aiqutepets.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WxConfig wxConfig;

    @Autowired
    private MpUserMapper mpUserMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WxService wxService;

    @Override
    public LoginResponse wxLogin(LoginRequest loginRequest) {
        // 1. 调用微信接口获取 openid 和 session_key
        WxSessionResponse wxSession = getWxSession(loginRequest.getCode());

        if (wxSession.getErrcode() != null && wxSession.getErrcode() != 0) {
            log.error("微信登录失败: errcode={}, errmsg={}", wxSession.getErrcode(), wxSession.getErrmsg());
            throw new RuntimeException("微信登录失败: " + wxSession.getErrmsg());
        }

        String openid = wxSession.getOpenid();
        String sessionKey = wxSession.getSessionKey();
        String unionid = wxSession.getUnionid();

        log.info("微信登录成功获取 openid: {}", openid);

        // 2. 查询用户是否存在
        MpUser user = mpUserMapper.selectByOpenid(openid);
        boolean isNewUser = false;

        if (user == null) {
            // 3a. 新用户 - 创建用户记录
            isNewUser = true;
            user = new MpUser();
            user.setOpenid(openid);
            user.setUnionid(unionid);
            user.setSessionKey(sessionKey);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            mpUserMapper.insert(user);
            log.info("创建新用户: id={}, openid={}", user.getId(), openid);
        } else {
            // 3b. 老用户 - 更新登录时间和 session_key
            user.setSessionKey(sessionKey);
            user.setUpdateTime(LocalDateTime.now());
            mpUserMapper.update(user);
            log.info("用户登录: id={}, openid={}", user.getId(), openid);
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), openid);

        // 5. 返回登录结果
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .isNewUser(isNewUser)
                .build();
    }

    @Override
    public String bindPhone(Long userId, String code) {
        // 1. 调用微信接口获取手机号
        String phone = wxService.getPhoneNumber(code);

        // 2. 更新用户手机号
        MpUser user = mpUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        mpUserMapper.update(user);

        log.info("用户 {} 绑定手机号成功: {}", userId, phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));

        return phone;
    }

    /**
     * 调用微信 jscode2session 接口
     *
     * @param code 微信小程序登录 code
     * @return 微信会话响应
     */
    private WxSessionResponse getWxSession(String code) {
        String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wxConfig.getJscode2sessionUrl(),
                wxConfig.getAppId(),
                wxConfig.getAppSecret(),
                code);

        log.debug("调用微信登录接口: {}", url);

        return restTemplate.getForObject(url, WxSessionResponse.class);
    }
}
