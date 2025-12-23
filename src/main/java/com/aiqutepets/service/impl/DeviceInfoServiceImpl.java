package com.aiqutepets.service.impl;

import com.aiqutepets.entity.DeviceInfo;
import com.aiqutepets.mapper.DeviceInfoMapper;
import com.aiqutepets.service.DeviceInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备信息 Service 实现类
 */
@Service
public class DeviceInfoServiceImpl implements DeviceInfoService {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Override
    public DeviceInfo getById(Long id) {
        return deviceInfoMapper.selectById(id);
    }

    @Override
    public DeviceInfo getByDeviceUid(String deviceUid) {
        return deviceInfoMapper.selectByDeviceUid(deviceUid);
    }

    @Override
    public List<DeviceInfo> listAll() {
        return deviceInfoMapper.selectAll();
    }

    @Override
    public boolean save(DeviceInfo deviceInfo) {
        return deviceInfoMapper.insert(deviceInfo) > 0;
    }

    @Override
    public boolean update(DeviceInfo deviceInfo) {
        return deviceInfoMapper.update(deviceInfo) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return deviceInfoMapper.deleteById(id) > 0;
    }
}
