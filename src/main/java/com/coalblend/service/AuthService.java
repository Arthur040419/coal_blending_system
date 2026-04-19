package com.coalblend.service;

import com.coalblend.dto.LoginDTO;
import com.coalblend.vo.UserVO;

public interface AuthService {

    UserVO login(LoginDTO dto);

    UserVO currentUser(Long userId);
}
