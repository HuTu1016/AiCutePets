package com.aiqutepets.mapper;

import com.aiqutepets.entity.AppRichContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * App 富文本内容 Mapper 接口
 */
@Mapper
public interface AppRichContentMapper {

    /**
     * 根据内容标识查询
     *
     * @param contentKey 内容标识 (如 guide, agreement)
     * @return 富文本内容
     */
    AppRichContent findByContentKey(@Param("contentKey") String contentKey);
}
