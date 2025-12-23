package com.aiqutepets.mapper;

import com.aiqutepets.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备信息 Mapper 接口
 */
@Mapper
public interface DeviceInfoMapper {

    /**
     * 根据ID查询设备
     */
    DeviceInfo selectById(@Param("id") Long id);

    /**
     * 根据设备UID查询设备
     */
    DeviceInfo selectByDeviceUid(@Param("deviceUid") String deviceUid);

    /**
     * 查询所有设备
     */
    List<DeviceInfo> selectAll();

    /**
     * 新增设备
     */
    int insert(DeviceInfo deviceInfo);

    /**
     * 更新设备
     */
    int update(DeviceInfo deviceInfo);

    /**
     * 根据ID删除设备
     */
    int deleteById(@Param("id") Long id);
}
