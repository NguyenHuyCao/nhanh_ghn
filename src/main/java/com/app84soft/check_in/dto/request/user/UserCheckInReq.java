package com.app84soft.check_in.dto.request.user;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCheckInReq {
    @NotNull(message = "Lỗi xử lý, vui lòng thoát app vào lại")
    String accessToken;
    @NotNull(message = "Không tìm thấy vị trí của bạn")
    String code;
    @NotNull(message = "Không tìm thấy mã lớp học")
    String courseCode;
}
