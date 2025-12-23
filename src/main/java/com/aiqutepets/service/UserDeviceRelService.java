package com.aiqutepets.service;

import com.aiqutepets.entity.UserDeviceRel;

import java.util.List;

/**
 * 用户设备绑定关系 Service 接口
 */
public interface UserDeviceRelService {

    /**
     * 根据ID查询关系
     */
    UserDeviceRel getById(Long id);

    /**
     * 根据用户ID查询绑定的设备列表
     */
    List<UserDeviceRel> listByUserId(Long userId);

    /**
     * 根据设备UID查询绑定的用户列表
     */
    List<UserDeviceRel> listByDeviceUid(String deviceUid);

    /**
     * 根据用户ID和设备UID查询关系
     */
    UserDeviceRel getByUserIdAndDeviceUid(Long userId, String deviceUid);

    /**
     * 查询所有关系
     */
    List<UserDeviceRel> listAll();

    /**
     * 新增绑定关系
     */
    boolean save(UserDeviceRel userDeviceRel);

    /**
     * 更新绑定关系
     */
    boolean update(UserDeviceRel userDeviceRel);

    /**
     * 根据ID删除关系
     */
    boolean removeById(Long id);

    /**
     * 根据用户ID和设备UID删除关系
     */
    boolean removeByUserIdAndDeviceUid(Long userId, String deviceUid);
}
