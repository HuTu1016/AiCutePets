package com.aiqutepets.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备动作日志表
 * 记录设备的各种动作（如 chat, play 等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceActionLog {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备UID
     */
    private String deviceUid;

    /**
     * 动作代码 (如 chat, play, story 等)
     */
    private String actionCode;

    /**
     * 持续时长或次数
     */
    private Integer durationOrCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
