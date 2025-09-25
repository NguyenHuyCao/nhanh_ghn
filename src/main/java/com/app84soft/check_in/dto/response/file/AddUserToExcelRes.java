package com.app84soft.check_in.dto.response.file;

import com.app84soft.check_in.dto.request.file.AddUserToExcelReq;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddUserToExcelRes{
    String excelId;
    @NotNull
    String name;
    String registerForm;
    String birthDay;
    String birthPlace;
    String job;
    String company;
    @NotNull
    String phone;
    @NotNull
    Integer courseId;
}
