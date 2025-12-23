package com.aiqutepets.dto;

import lombok.Data;

/**
 * 记忆页面日历数据 DTO
 * 用于前端日历组件展示
 */
@Data
public class MemoryCalendarDTO {

    /**
     * 日期
     */
    private String date;

    /**
     * 是否有点标记 (对应 has_diary)
     */
    private Boolean hasDot;

    /**
     * 状态
     */
    private String status;
}
