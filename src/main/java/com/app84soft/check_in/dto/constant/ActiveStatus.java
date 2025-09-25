package com.app84soft.check_in.dto.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ActiveStatus implements BaseEnum<Integer> {
    INACTIVE(0),
    ACTIVE(1);

    private final int value;

    ActiveStatus(int value) {
        this.value = value;
    }

    @JsonCreator
    public static ActiveStatus fromValue(int value) {
        for (ActiveStatus column : values()) {
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
