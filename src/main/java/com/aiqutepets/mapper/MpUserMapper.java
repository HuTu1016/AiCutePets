package com.aiqutepets.mapper;

import com.aiqutepets.entity.MpUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小程序用户 Mapper 接口
 */
@Mapper
public interface MpUserMapper {

    /**
     * 根据ID查询用户
     */
    MpUser selectById(@Param("id") Long id);

    /**
     * 根据OpenID查询用户
     */
    MpUser selectByOpenid(@Param("openid") String openid);

    /**
     * 查询所有用户
     */
    List<MpUser> selectAll();

    /**
     * 新增用户
     */
    int insert(MpUser mpUser);

    /**
     * 更新用户
     */
    int update(MpUser mpUser);

    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Long id);
}
