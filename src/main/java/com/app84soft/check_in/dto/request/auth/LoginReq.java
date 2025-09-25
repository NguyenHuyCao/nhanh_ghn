package com.app84soft.check_in.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReq {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
