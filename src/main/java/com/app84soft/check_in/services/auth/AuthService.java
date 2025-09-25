package com.app84soft.check_in.services.auth;

import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.user.User;

public interface AuthService {
    UserLoginRes login(LoginReq req);
    User register(RegisterReq req);
    User createUserByAdmin(CreateUserByAdminReq req);
}
