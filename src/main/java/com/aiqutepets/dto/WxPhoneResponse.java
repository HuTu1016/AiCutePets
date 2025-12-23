package com.aiqutepets.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信获取手机号接口响应
 */
@Data
public class WxPhoneResponse {

    /**
     * 错误码
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 手机号信息
     */
    @JsonProperty("phone_info")
    private PhoneInfo phoneInfo;

    @Data
    public static class PhoneInfo {
        /**
         * 用户绑定的手机号（国外手机号会有区号）
         */
        private String phoneNumber;

        /**
         * 没有区号的手机号
         */
        private String purePhoneNumber;

        /**
         * 区号
         */
        private String countryCode;

        /**
         * 数据水印
         */
        private Watermark watermark;
    }

    @Data
    public static class Watermark {
        /**
         * 用户获取手机号操作的时间戳
         */
        private Long timestamp;

        /**
         * 小程序appid
         */
        private String appid;
    }
}
