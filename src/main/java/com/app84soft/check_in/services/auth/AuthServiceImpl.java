package com.app84soft.check_in.services.auth;

import com.app84soft.check_in.dto.constant.ActiveStatus;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.request.user.UpdateUserReq;
import com.app84soft.check_in.dto.request.user.UpdateUserRequest;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.auth.RefreshToken;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.repositories.auth.RefreshTokenRepository;
import com.app84soft.check_in.repositories.user.UserRepository;
import com.app84soft.check_in.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserLoginRes login(LoginReq req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException("Sai email hoặc mật khẩu", HttpStatus.UNAUTHORIZED));

        if (user.isDeleted() || user.getStatus() == ActiveStatus.INACTIVE) {
            throw new BusinessException("Tài khoản bị khoá", HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("Sai email hoặc mật khẩu", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtTokenProvider.generateToken(String.valueOf(user.getId()));

        return UserLoginRes.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .token(token)
                .build();
    }

    @Override
    public User register(RegisterReq req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email đã tồn tại", HttpStatus.BAD_REQUEST);
        }
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRoleType(RoleType.USER);
        u.setStatus(ActiveStatus.ACTIVE);
        return userRepository.save(u);
    }

    @Override
    public User createUserByAdmin(CreateUserByAdminReq req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email đã tồn tại", HttpStatus.BAD_REQUEST);
        }
        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRoleType(RoleType.USER);
        u.setStatus(ActiveStatus.ACTIVE);
        return userRepository.save(u);
    }

    // AuthServiceImpl.java
    @Override
    public User updateUserByAdmin(Integer userId, UpdateUserReq req) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User không tồn tại", HttpStatus.NOT_FOUND));

        if (req.getName() != null) {
            u.setName(req.getName());
        }

        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(u.getEmail())) {
            // tìm user khác đang dùng email này
            userRepository.findByEmail(req.getEmail())
                    .filter(exist -> exist.getId() != u.getId())   // <<== so sánh int, KHÔNG dùng equals
                    .ifPresent(x -> { throw new BusinessException("Email đã tồn tại", HttpStatus.BAD_REQUEST); });
            u.setEmail(req.getEmail());
        }

        if (req.getPhone() != null) {
            u.setPhone(req.getPhone());
        }

        if (req.getStatus() != null) {
            u.setStatus(req.getStatus() == 1 ? ActiveStatus.ACTIVE : ActiveStatus.INACTIVE);
        }

        return userRepository.save(u);
    }

    @Override
    public void deleteUserByAdmin(Integer userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User không tồn tại", HttpStatus.NOT_FOUND));
        u.setDeleted(true);                 // soft delete
        userRepository.save(u);
    }

    // để dùng tạm thời ở bước 6 (bước 7 sẽ bổ sung refresh token/rotate/revoke)
    @Override
    public String issueAccessToken(User user) {
        return jwtTokenProvider.generateToken(String.valueOf(user.getId()));
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public RefreshToken issueRefreshToken(User user, String ua, String ip) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(java.util.UUID.randomUUID().toString()); // hoặc random 256-bit base64
        rt.setExpiresAt(java.time.Instant.now().plusSeconds(60L * 60L * 24L * 30L)); // 30 ngày
        rt.setUserAgent(ua);
        rt.setIp(ip);
        return refreshTokenRepository.save(rt);
    }
    @org.springframework.transaction.annotation.Transactional
    @Override
    public Map<String, String> rotate(String refreshToken, String ua, String ip) {
        RefreshToken old = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED));
        if (old.getRevokedAt()!=null || java.time.Instant.now().isAfter(old.getExpiresAt())) {
            throw new BusinessException("Refresh token đã hết hạn/đã thu hồi", HttpStatus.UNAUTHORIZED);
        }

        // cấp access token mới
        User user = old.getUser();
        String access = jwtTokenProvider.generateToken(String.valueOf(user.getId()));

        // cấp refresh token mới và thu hồi cái cũ (rotate)
        RefreshToken neo = issueRefreshToken(user, ua, ip);
        old.setRevokedAt(java.time.Instant.now());
        old.setReplacedBy(neo.getToken());
        refreshTokenRepository.save(old);

        return java.util.Map.of(
                "access_token", access,
                "refresh_token", neo.getToken()
        );
    }

    @org.springframework.transaction.annotation.Transactional
    @Override
    public void revoke(String refreshToken) {
        RefreshToken t = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Refresh token không hợp lệ", HttpStatus.BAD_REQUEST));
        if (t.getRevokedAt()==null) {
            t.setRevokedAt(java.time.Instant.now());
            refreshTokenRepository.save(t);
        }
    }


}
