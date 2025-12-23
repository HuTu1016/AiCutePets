package com.aiqutepets.service;

import com.aiqutepets.dto.ContentDetailResponse;

/**
 * 内容管理服务接口
 */
public interface ContentService {

    /**
     * 根据 key 获取富文本内容
     *
     * @param key 内容标识 (如 guide, agreement)
     * @return 内容详情
     */
    ContentDetailResponse getContentByKey(String key);

    /**
     * 清除内容缓存（当后台更新内容时调用）
     *
     * @param key 内容标识，传 null 则清除所有
     */
    void clearCache(String key);
}
