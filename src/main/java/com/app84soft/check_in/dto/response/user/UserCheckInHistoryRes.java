package com.app84soft.check_in.dto.response.user;

import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.*;
import lombok.experimental.FieldDefaults;


import java.time.LocalDate;
import java.util.Date;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCheckInHistoryRes {

    int checkInId; // session student id
    int userId;
    int courseId;
    String courseName;
    String courseCode;
    int sessionId;

    @Temporal(TemporalType.DATE)
    Date sessionSchedule;

    @Temporal(TemporalType.TIMESTAMP)
    private Date checkInTime;

}
