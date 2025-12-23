package com.aiqutepets.service.impl;

import com.aiqutepets.dto.ContentDetailResponse;
import com.aiqutepets.entity.AppRichContent;
import com.aiqutepets.mapper.AppRichContentMapper;
import com.aiqutepets.service.ContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内容管理服务实现类
 * 带有简单的内存缓存，避免每次都查数据库
 */
@Slf4j
@Service
public class ContentServiceImpl implements ContentService {

    @Autowired
    private AppRichContentMapper appRichContentMapper;

    /**
     * 内存缓存：key -> ContentDetailResponse
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, ContentDetailResponse> contentCache = new ConcurrentHashMap<>();

    @Override
    public ContentDetailResponse getContentByKey(String key) {
        log.info("获取富文本内容: key={}", key);

        // 1. 先从缓存获取
        ContentDetailResponse cached = contentCache.get(key);
        if (cached != null) {
            log.debug("命中缓存: key={}", key);
            return cached;
        }

        // 2. 缓存未命中，查询数据库
        AppRichContent content = appRichContentMapper.findByContentKey(key);

        if (content == null) {
            log.warn("内容不存在: key={}", key);
            return null;
        }

        // 3. 构建响应对象
        ContentDetailResponse response = ContentDetailResponse.builder()
                .title(content.getTitle())
                .content(content.getContentHtml())
                .build();

        // 4. 存入缓存
        contentCache.put(key, response);
        log.info("内容已加载到缓存: key={}", key);

        return response;
    }

    @Override
    public void clearCache(String key) {
        if (key == null) {
            contentCache.clear();
            log.info("已清除所有内容缓存");
        } else {
            contentCache.remove(key);
            log.info("已清除内容缓存: key={}", key);
        }
    }
}
