package com.app84soft.check_in.dto.response.user;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserCheckInRes {
    int userId;
    int courseId;
    int lessonId;
    String name;
    String excelName;
    Boolean isCheckIn;
}
