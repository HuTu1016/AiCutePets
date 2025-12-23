package com.aiqutepets.mapper;

import com.aiqutepets.entity.DeviceActionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备动作日志 Mapper 接口
 */
@Mapper
public interface DeviceActionLogMapper {

    /**
     * 插入动作日志
     */
    int insert(DeviceActionLog actionLog);

    /**
     * 根据设备UID查询动作日志
     */
    List<DeviceActionLog> selectByDeviceUid(@Param("deviceUid") String deviceUid);

    /**
     * 根据设备UID和动作代码查询
     */
    List<DeviceActionLog> selectByDeviceUidAndActionCode(
            @Param("deviceUid") String deviceUid,
            @Param("actionCode") String actionCode);

    /**
     * 根据时间范围查询
     */
    List<DeviceActionLog> selectByTimeRange(
            @Param("deviceUid") String deviceUid,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计设备的动作次数
     */
    int countByDeviceUid(@Param("deviceUid") String deviceUid);
}
