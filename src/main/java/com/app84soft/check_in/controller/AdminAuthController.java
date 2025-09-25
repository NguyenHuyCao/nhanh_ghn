package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.request.admin.CreateUserByAdminReq;
import com.app84soft.check_in.dto.request.user.UpdateUserReq;
import com.app84soft.check_in.dto.response.BaseResponse;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.services.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AuthService authService;

    @PostMapping("/users")
    @Operation(summary = "Admin tạo tài khoản user")
    public ResponseEntity<BaseResponse<User>> createUserByAdmin(@RequestBody @Valid CreateUserByAdminReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.createUserByAdmin(req)));
    }

    @PatchMapping("/users/{id}")
    @Operation(summary = "Admin cập nhật user")
    public ResponseEntity<BaseResponse<User>> updateUser(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateUserReq req) {
        return ResponseEntity.ok(new BaseResponse<>(authService.updateUserByAdmin(id, req)));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Admin xoá user (soft delete)")
    public ResponseEntity<BaseResponse<Boolean>> deleteUser(@PathVariable Integer id) {
        authService.deleteUserByAdmin(id);
        return ResponseEntity.ok(new BaseResponse<>(true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<Map<String,String>>> refresh(@RequestBody Map<String,String> body,
                                                                    HttpServletRequest req) {
        String rt = body.get("refresh_token");
        var res = authService.rotate(rt, req.getHeader("User-Agent"), req.getRemoteAddr());
        return ResponseEntity.ok(new BaseResponse<>(res));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Boolean>> logout(@RequestBody Map<String,String> body) {
        authService.revoke(body.get("refresh_token"));
        return ResponseEntity.ok(new BaseResponse<>(true));
    }

}
