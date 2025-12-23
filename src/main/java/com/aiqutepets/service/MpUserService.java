package com.aiqutepets.service;

import com.aiqutepets.entity.MpUser;

import java.util.List;

/**
 * 小程序用户 Service 接口
 */
public interface MpUserService {

    /**
     * 根据ID查询用户
     */
    MpUser getById(Long id);

    /**
     * 根据OpenID查询用户
     */
    MpUser getByOpenid(String openid);

    /**
     * 查询所有用户
     */
    List<MpUser> listAll();

    /**
     * 新增用户
     */
    boolean save(MpUser mpUser);

    /**
     * 更新用户
     */
    boolean update(MpUser mpUser);

    /**
     * 根据ID删除用户
     */
    boolean removeById(Long id);
}
