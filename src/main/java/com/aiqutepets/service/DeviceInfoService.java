package com.aiqutepets.service;

import com.aiqutepets.entity.DeviceInfo;

import java.util.List;

/**
 * 设备信息 Service 接口
 */
public interface DeviceInfoService {

    /**
     * 根据ID查询设备
     */
    DeviceInfo getById(Long id);

    /**
     * 根据设备UID查询设备
     */
    DeviceInfo getByDeviceUid(String deviceUid);

    /**
     * 查询所有设备
     */
    List<DeviceInfo> listAll();

    /**
     * 新增设备
     */
    boolean save(DeviceInfo deviceInfo);

    /**
     * 更新设备
     */
    boolean update(DeviceInfo deviceInfo);

    /**
     * 根据ID删除设备
     */
    boolean removeById(Long id);
}
