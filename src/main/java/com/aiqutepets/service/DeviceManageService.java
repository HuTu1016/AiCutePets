package com.aiqutepets.service;

import com.aiqutepets.dto.DeviceBindRequest;
import com.aiqutepets.dto.DeviceBindResponse;
import com.aiqutepets.dto.DeviceCheckResponse;
import com.aiqutepets.dto.DeviceUpdateRequest;
import com.aiqutepets.dto.MyDeviceDTO;

/**
 * 设备管理服务接口
 */
public interface DeviceManageService {

    /**
     * 校验设备合法性
     *
     * @param deviceUid 设备唯一标识
     * @return 校验结果
     */
    DeviceCheckResponse checkDeviceValid(String deviceUid);

    /**
     * 绑定设备
     *
     * @param userId  当前用户ID
     * @param request 绑定请求
     * @return 绑定结果
     */
    DeviceBindResponse bindDevice(Long userId, DeviceBindRequest request);

    /**
     * 获取设备详情
     *
     * @param userId    当前用户ID
     * @param deviceUid 设备唯一标识
     * @return 设备详情
     */
    MyDeviceDTO getDeviceDetail(Long userId, String deviceUid);

    /**
     * 更新设备信息（昵称、头像）
     *
     * @param userId  当前用户ID
     * @param request 更新请求
     * @return 是否更新成功
     */
    boolean updateDevice(Long userId, DeviceUpdateRequest request);

    /**
     * 解除设备绑定
     *
     * @param userId    当前用户ID
     * @param deviceUid 设备唯一标识
     * @return 解绑结果消息（如果是管理员解绑会返回提示）
     */
    String unbindDevice(Long userId, String deviceUid);

    /**
     * 检查设备固件更新
     *
     * @param userId    当前用户ID
     * @param deviceUid 设备唯一标识
     * @return 固件更新状态
     */
    com.aiqutepets.dto.FirmwareCheckResponse checkFirmwareUpdate(Long userId, String deviceUid);

    /**
     * 刷新设备在线状态
     *
     * @param userId    当前用户ID
     * @param deviceUid 设备唯一标识
     * @return 设备状态信息
     */
    com.aiqutepets.dto.DeviceStatusResponse refreshDeviceStatus(Long userId, String deviceUid);

    /**
     * 获取设备列表（用于首页设备切换）
     *
     * @param userId 当前用户ID
     * @return 设备列表
     */
    java.util.List<com.aiqutepets.dto.DeviceListDTO> getDeviceList(Long userId);

    /**
     * 切换当前设备
     *
     * @param userId    当前用户ID
     * @param deviceUid 设备唯一标识
     * @return 是否切换成功
     */
    boolean switchDevice(Long userId, String deviceUid);
}
