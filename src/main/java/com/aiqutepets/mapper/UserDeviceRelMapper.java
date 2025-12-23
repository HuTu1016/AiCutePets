package com.aiqutepets.mapper;

import com.aiqutepets.dto.DeviceListDTO;
import com.aiqutepets.dto.MyDeviceDTO;
import com.aiqutepets.entity.UserDeviceRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户设备绑定关系 Mapper 接口
 */
@Mapper
public interface UserDeviceRelMapper {

    /**
     * 根据ID查询关系
     */
    UserDeviceRel selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询绑定的设备列表
     */
    List<UserDeviceRel> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据设备UID查询绑定的用户列表
     */
    List<UserDeviceRel> selectByDeviceUid(@Param("deviceUid") String deviceUid);

    /**
     * 根据用户ID和设备UID查询关系
     */
    UserDeviceRel selectByUserIdAndDeviceUid(@Param("userId") Long userId, @Param("deviceUid") String deviceUid);

    /**
     * 查询所有关系
     */
    List<UserDeviceRel> selectAll();

    /**
     * 新增绑定关系
     */
    int insert(UserDeviceRel userDeviceRel);

    /**
     * 更新绑定关系
     */
    int update(UserDeviceRel userDeviceRel);

    /**
     * 根据ID删除关系
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID和设备UID删除关系
     */
    int deleteByUserIdAndDeviceUid(@Param("userId") Long userId, @Param("deviceUid") String deviceUid);

    /**
     * 查询用户的设备列表 (联表查询，包含设备详细信息)
     * 
     * @param userId 用户ID
     * @return 设备详细信息列表
     */
    List<MyDeviceDTO> selectMyDevices(@Param("userId") Long userId);

    /**
     * 查询用户的单个设备详情 (联表查询)
     * 
     * @param userId    用户ID
     * @param deviceUid 设备UID
     * @return 设备详细信息
     */
    MyDeviceDTO selectMyDeviceDetail(@Param("userId") Long userId, @Param("deviceUid") String deviceUid);

    /**
     * 查询我的设备列表（用于首页设备切换）
     * 排序：优先按 is_current 降序，其次按 last_used_time 降序
     *
     * @param userId 用户ID
     * @return 设备列表
     */
    List<DeviceListDTO> selectDeviceList(@Param("userId") Long userId);

    /**
     * 重置当前设备状态（将该用户所有设备的 is_current 设为 0）
     *
     * @param userId 用户ID
     */
    void clearCurrentDevice(@Param("userId") Long userId);

    /**
     * 设置新的当前设备
     *
     * @param userId    用户ID
     * @param deviceUid 设备UID
     */
    void setCurrentDevice(@Param("userId") Long userId, @Param("deviceUid") String deviceUid);

    /**
     * 查询用户当前选中的设备
     *
     * @param userId 用户ID
     * @return 当前选中设备的绑定关系，如果没有则返回 null
     */
    UserDeviceRel selectCurrentDevice(@Param("userId") Long userId);

    /**
     * 更新设备的OTA更新标记（用于首页红点缓存）
     *
     * @param deviceUid    设备UID
     * @param hasOtaUpdate OTA更新标记 (0=无更新, 1=有更新)
     * @return 影响行数
     */
    int updateOtaUpdateFlag(@Param("deviceUid") String deviceUid,
            @Param("hasOtaUpdate") Integer hasOtaUpdate);
}
