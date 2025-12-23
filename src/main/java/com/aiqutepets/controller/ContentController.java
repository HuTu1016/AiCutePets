package com.aiqutepets.controller;

import com.aiqutepets.common.Result;
import com.aiqutepets.dto.ContentDetailResponse;
import com.aiqutepets.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通用内容管理控制器
 * 用于前端获取富文本内容（玩伴指南、陪伴约定等）
 * 此接口不需要登录鉴权
 */
@Slf4j
@RestController
@RequestMapping("/api/content")
@Tag(name = "内容接口", description = "获取富文本内容（玩伴指南、陪伴约定等），无需鉴权")
public class ContentController {

    @Autowired
    private ContentService contentService;

    /**
     * 获取内容详情
     * 不需要登录鉴权
     *
     * @param key 内容标识 (如 guide, agreement)
     * @return 内容详情
     */
    @Operation(summary = "获取内容详情", description = "根据 key 获取富文本内容，无需鉴权")
    @GetMapping("/detail")
    public Result<ContentDetailResponse> getContentDetail(
            @Parameter(description = "内容标识，如 guide(玩伴指南)、agreement(陪伴约定)", required = true, example = "guide") @RequestParam("key") String key) {
        log.info("收到获取内容请求: key={}", key);

        if (key == null || key.isEmpty()) {
            return Result.error(400, "key 不能为空");
        }

        try {
            ContentDetailResponse response = contentService.getContentByKey(key);

            if (response == null) {
                return Result.error(404, "内容不存在");
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("获取内容失败", e);
            return Result.error("获取内容失败: " + e.getMessage());
        }
    }

    /**
     * 清除内容缓存（管理后台调用）
     * 注意：此接口建议限制为管理员访问
     *
     * @param key 内容标识，不传则清除所有
     * @return 操作结果
     */
    @Operation(summary = "清除内容缓存", description = "管理后台调用，清除指定或所有内容缓存")
    @PostMapping("/cache/clear")
    public Result<Void> clearCache(
            @Parameter(description = "内容标识，不传则清除所有缓存", required = false) @RequestParam(value = "key", required = false) String key) {
        log.info("收到清除缓存请求: key={}", key);
        contentService.clearCache(key);
        return Result.success();
    }
}
