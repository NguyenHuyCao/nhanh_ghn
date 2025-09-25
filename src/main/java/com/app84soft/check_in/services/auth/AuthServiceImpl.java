package com.app84soft.check_in.services.auth;

import com.app84soft.check_in.dto.constant.ActiveStatus;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.request.auth.LoginReq;
import com.app84soft.check_in.dto.request.auth.RegisterReq;
import com.app84soft.check_in.dto.response.user.UserLoginRes;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.repositories.user.UserRepository;
import com.app84soft.check_in.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
}
