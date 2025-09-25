package com.app84soft.check_in.dto.response.user;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDetailRes {
    String firstName;
    String lastName;
    String phone;
    String studentId;
    String accessToken;
}
