package com.coalblend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coalblend.common.exception.BusinessException;
import com.coalblend.dto.LoginDTO;
import com.coalblend.entity.SysUser;
import com.coalblend.mapper.SysUserMapper;
import com.coalblend.service.AuthService;
import com.coalblend.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    @Override
    public UserVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null || !Objects.equals(user.getPassword(), dto.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已禁用");
        }
        return UserVO.from(user);
    }

    @Override
    public UserVO currentUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "未登录或缺少 X-User-Id");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserVO.from(user);
    }
}
