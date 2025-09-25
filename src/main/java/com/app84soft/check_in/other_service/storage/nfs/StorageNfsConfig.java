package com.app84soft.check_in.other_service.storage.nfs;

import com.app84soft.check_in.other_service.storage.StorageConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StorageNfsConfig implements StorageConfig {
    private String directory;
    private String serverUrl;
}

