package com.app84soft.check_in.dto.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoleType implements BaseEnum<Integer> {
    ADMIN(0),
    USER(1),
    STUDENT(2);

    private final int value;

    RoleType(int value) {
        this.value = value;
    }

    @JsonCreator
    public static RoleType fromValue(int value) {
        for (RoleType column : values()) {
            if (column.toValue() == value) {
                return column;
            }
        }
        return null;
    }

    @Override
    public Integer toValue() {
        return value;
    }
}
