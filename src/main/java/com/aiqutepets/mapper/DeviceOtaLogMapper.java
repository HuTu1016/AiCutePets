package com.aiqutepets.mapper;

import com.aiqutepets.entity.DeviceOtaLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备OTA日志 Mapper
 */
@Mapper
public interface DeviceOtaLogMapper {

    /**
     * 插入OTA操作日志
     *
     * @param log 日志实体
     * @return 影响行数
     */
    int insert(DeviceOtaLog log);

    /**
     * 根据设备UID查询日志列表
     *
     * @param deviceUid 设备UID
     * @return 日志列表
     */
    List<DeviceOtaLog> selectByDeviceUid(@Param("deviceUid") String deviceUid);

    /**
     * 根据设备UID和操作类型查询最新一条日志
     *
     * @param deviceUid  设备UID
     * @param actionType 操作类型
     * @return 最新日志记录
     */
    DeviceOtaLog selectLatestByDeviceUidAndAction(@Param("deviceUid") String deviceUid,
            @Param("actionType") Integer actionType);
}
