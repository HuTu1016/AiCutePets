package com.aiqutepets.service.impl;

import com.aiqutepets.entity.UserDeviceRel;
import com.aiqutepets.mapper.UserDeviceRelMapper;
import com.aiqutepets.service.UserDeviceRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户设备绑定关系 Service 实现类
 */
@Service
public class UserDeviceRelServiceImpl implements UserDeviceRelService {

    @Autowired
    private UserDeviceRelMapper userDeviceRelMapper;

    @Override
    public UserDeviceRel getById(Long id) {
        return userDeviceRelMapper.selectById(id);
    }

    @Override
    public List<UserDeviceRel> listByUserId(Long userId) {
        return userDeviceRelMapper.selectByUserId(userId);
    }

    @Override
    public List<UserDeviceRel> listByDeviceUid(String deviceUid) {
        return userDeviceRelMapper.selectByDeviceUid(deviceUid);
    }

    @Override
    public UserDeviceRel getByUserIdAndDeviceUid(Long userId, String deviceUid) {
        return userDeviceRelMapper.selectByUserIdAndDeviceUid(userId, deviceUid);
    }

    @Override
    public List<UserDeviceRel> listAll() {
        return userDeviceRelMapper.selectAll();
    }

    @Override
    public boolean save(UserDeviceRel userDeviceRel) {
        return userDeviceRelMapper.insert(userDeviceRel) > 0;
    }

    @Override
    public boolean update(UserDeviceRel userDeviceRel) {
        return userDeviceRelMapper.update(userDeviceRel) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return userDeviceRelMapper.deleteById(id) > 0;
    }

    @Override
    public boolean removeByUserIdAndDeviceUid(Long userId, String deviceUid) {
        return userDeviceRelMapper.deleteByUserIdAndDeviceUid(userId, deviceUid) > 0;
    }

    @Override
    public boolean unbindAllByDevice(String deviceUid) {
        int affected = userDeviceRelMapper.unbindAllByDeviceUid(deviceUid);
        return affected >= 0;
    }
}
