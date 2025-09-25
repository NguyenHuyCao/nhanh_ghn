package com.app84soft.check_in.dto.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StorageType {
    STORAGE_NFS_LOCAL;

    public static StorageType getType(int index) {
        return StorageType.values()[index];
    }
    public static StorageType getType(String name) {
        return StorageType.valueOf(name);
    }

    @JsonValue
    public int toValue() {
        return ordinal();
    }
}
