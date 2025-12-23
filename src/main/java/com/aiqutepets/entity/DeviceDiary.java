package com.aiqutepets.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备日记历史表
 */
@Data
public class DeviceDiary {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备唯一标识
     */
    private String deviceUid;

    /**
     * 日记日期
     */
    private LocalDate diaryDate;

    /**
     * 日记正文内容
     */
    private String content;

    /**
     * 情绪标签 (JSON 数组字符串)
     */
    private String emotionTags;

    /**
     * AI 生成时间
     */
    private LocalDateTime aiGeneratedTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
