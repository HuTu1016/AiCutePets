package com.aiqutepets.service.impl;

import com.aiqutepets.entity.MpUser;
import com.aiqutepets.mapper.MpUserMapper;
import com.aiqutepets.service.MpUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 小程序用户 Service 实现类
 */
@Service
public class MpUserServiceImpl implements MpUserService {

    @Autowired
    private MpUserMapper mpUserMapper;

    @Override
    public MpUser getById(Long id) {
        return mpUserMapper.selectById(id);
    }

    @Override
    public MpUser getByOpenid(String openid) {
        return mpUserMapper.selectByOpenid(openid);
    }

    @Override
    public List<MpUser> listAll() {
        return mpUserMapper.selectAll();
    }

    @Override
    public boolean save(MpUser mpUser) {
        return mpUserMapper.insert(mpUser) > 0;
    }

    @Override
    public boolean update(MpUser mpUser) {
        return mpUserMapper.update(mpUser) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return mpUserMapper.deleteById(id) > 0;
    }
}
