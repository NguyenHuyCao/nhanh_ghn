package com.app84soft.check_in.dto.request.user;

import jakarta.validation.constraints.Email;
import lombok.Data;
@Data
public class UpdateUserReq {
    private String name;
    @Email private String email;
    private String phone;
    private Integer status;
}
