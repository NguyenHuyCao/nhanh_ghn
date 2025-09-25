package com.app84soft.check_in.services.auth;

import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.request.user.UpdateUserReq;
import com.app84soft.check_in.dto.request.user.UpdateUserRequest;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.auth.RefreshToken;
import com.app84soft.check_in.entities.user.User;

import java.util.Map;

public interface AuthService {
    UserLoginRes login(LoginReq req);
    User register(RegisterReq req);
    User createUserByAdmin(CreateUserByAdminReq req);

    User updateUserByAdmin(Integer userId, UpdateUserReq req);
    void deleteUserByAdmin(Integer userId);

    String issueAccessToken(User user);  // JWT ngắn hạn
    RefreshToken issueRefreshToken(User user, String ua, String ip); // random + lưu DB
    Map<String,String> rotate(String refreshToken, String ua, String ip); // trả {access_token, refresh_token}
    void revoke(String refreshToken);
}
