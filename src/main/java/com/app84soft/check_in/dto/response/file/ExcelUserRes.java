package com.app84soft.check_in.dto.response.file;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExcelUserRes {
    String numOrder;
    String formRegister;
    String company;
    String name;
    String birthday;
    String birthplace;
    String job;
    String phone;
    String registerForm;
}
