package com.coalblend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coalblend.entity.SysUser;

public interface UserService {

    IPage<SysUser> page(long current, long size, String keyword, String role, Integer status);

    void add(SysUser entity);

    void update(SysUser entity);

    void updateStatus(Long id, Integer status);

    SysUser getById(Long id);

    void delete(Long id);
}
