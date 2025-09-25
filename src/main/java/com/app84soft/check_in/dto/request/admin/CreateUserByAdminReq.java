package com.app84soft.check_in.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserByAdminReq {
    @NotBlank
    String name;

    @Email
    String email;

    @NotBlank
    String phone;

    @NotBlank
    String password;
}

