package com.coalblend.vo;

import com.coalblend.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private String email;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static UserVO from(SysUser u) {
        if (u == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setRole(u.getRole());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        vo.setUpdateTime(u.getUpdateTime());
        return vo;
    }
}
