package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.entity.SysUser;
import com.coalblend.mapper.SysUserMapper;
import com.coalblend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    @Override
    public IPage<SysUser> page(long current, long size, String keyword, String role, Integer status) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (StringUtils.hasText(role)) {
            w.eq(SysUser::getRole, role);
        }
        if (status != null) {
            w.eq(SysUser::getStatus, status);
        }
        w.orderByDesc(SysUser::getId);
        return sysUserMapper.selectPage(page, w);
    }

    @Override
    public void add(SysUser entity) {
        if (!StringUtils.hasText(entity.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(entity.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        Long cnt = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, entity.getUsername()));
        if (cnt != null && cnt > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (entity.getRole() == null) {
            entity.setRole("user");
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        sysUserMapper.insert(entity);
    }

    @Override
    public void update(SysUser entity) {
        if (entity.getId() == null) {
            throw new BusinessException("id 不能为空");
        }
        SysUser db = sysUserMapper.selectById(entity.getId());
        if (db == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (StringUtils.hasText(entity.getUsername()) && !entity.getUsername().equals(db.getUsername())) {
            Long cnt = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, entity.getUsername()));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("用户名已存在");
            }
            db.setUsername(entity.getUsername());
        }
        if (StringUtils.hasText(entity.getPassword())) {
            db.setPassword(entity.getPassword());
        }
        if (entity.getRealName() != null) {
            db.setRealName(entity.getRealName());
        }
        if (entity.getRole() != null) {
            db.setRole(entity.getRole());
        }
        if (entity.getPhone() != null) {
            db.setPhone(entity.getPhone());
        }
        if (entity.getEmail() != null) {
            db.setEmail(entity.getEmail());
        }
        if (entity.getStatus() != null) {
            db.setStatus(entity.getStatus());
        }
        sysUserMapper.updateById(db);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser old = sysUserMapper.selectById(id);
        if (old == null) {
            throw new BusinessException(404, "用户不存在");
        }
        SysUser u = new SysUser();
        u.setId(id);
        u.setStatus(status);
        sysUserMapper.updateById(u);
    }

    @Override
    public SysUser getById(Long id) {
        SysUser row = sysUserMapper.selectById(id);
        if (row == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return row;
    }

    @Override
    public void delete(Long id) {
        getById(id);
        sysUserMapper.deleteById(id);
    }
}
