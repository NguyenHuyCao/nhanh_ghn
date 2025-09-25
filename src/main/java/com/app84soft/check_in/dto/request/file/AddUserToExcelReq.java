package com.app84soft.check_in.dto.request.file;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddUserToExcelReq {
    @NotNull
    String name;
    String registerForm = "";
    String birthDay = "";
    String birthPlace = "";
    String job = "";
    String company = "";
    @NotNull
    String phone;
    @NotNull
    Integer courseId;

}
