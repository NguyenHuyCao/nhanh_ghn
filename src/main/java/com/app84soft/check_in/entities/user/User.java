package com.app84soft.check_in.entities.user;

import com.app84soft.check_in.component.converter.DataSensitiveConverter;
import com.app84soft.check_in.dto.constant.ActiveStatus;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.entities.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends BaseEntity {
    @NotNull
    String name;

    @Convert(converter = DataSensitiveConverter.class)
    String phone;

    @Email
    @Column(nullable = false, unique = true)
    String email;

    @JsonIgnore
    @Column(nullable = false)
    String password;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role_type", nullable = false, columnDefinition = "INT")
    RoleType roleType = RoleType.USER; // ADMIN/USER/STUDENT

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, columnDefinition = "INT")
    ActiveStatus status = ActiveStatus.ACTIVE;

    Integer failedLoginAttempts;
    Instant lockedUntil;
    Instant lastLoginAt;
}
