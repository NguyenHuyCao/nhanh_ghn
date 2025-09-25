package com.app84soft.check_in.dto.request.user;

import com.app84soft.check_in.dto.constant.ActiveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {
    @NotNull
    Integer userId;
    String name;
    ActiveStatus status;
}
