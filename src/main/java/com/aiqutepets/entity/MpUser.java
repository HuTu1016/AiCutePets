package com.aiqutepets.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 小程序用户表
 */
@Data
public class MpUser {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 微信OpenID (唯一标识)
     */
    private String openid;

    /**
     * 微信UnionID
     */
    private String unionid;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 会话密钥 (后端缓存用)
     */
    private String sessionKey;

    /**
     * 注册时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
