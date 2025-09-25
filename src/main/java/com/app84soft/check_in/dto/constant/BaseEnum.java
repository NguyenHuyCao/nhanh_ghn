package com.app84soft.check_in.dto.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public interface BaseEnum<T> {
     @JsonValue
     T toValue();
}
