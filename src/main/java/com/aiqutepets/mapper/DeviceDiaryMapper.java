package com.aiqutepets.mapper;

import com.aiqutepets.entity.DeviceDiary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备日记 Mapper 接口
 */
@Mapper
public interface DeviceDiaryMapper {

    /**
     * 根据设备UID和日期查询日记
     *
     * @param deviceUid 设备UID
     * @param diaryDate 日记日期 (yyyy-MM-dd)
     * @return 日记缓存记录
     */
    DeviceDiary selectByDeviceUidAndDate(@Param("deviceUid") String deviceUid,
            @Param("diaryDate") String diaryDate);

    /**
     * 插入日记记录
     *
     * @param diary 日记实体
     * @return 影响行数
     */
    int insert(DeviceDiary diary);

    /**
     * 更新日记记录
     *
     * @param diary 日记实体
     * @return 影响行数
     */
    int update(DeviceDiary diary);
}
