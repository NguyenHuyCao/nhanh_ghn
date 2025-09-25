package com.app84soft.check_in.other_service.storage;

import com.app84soft.check_in.dto.constant.StorageType;
import com.app84soft.check_in.other_service.storage.nfs.StorageLocal;
import com.app84soft.check_in.other_service.storage.nfs.StorageNfsConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Configuration
public class FileStorageConfig  {
    @Value("${file.storage-type}")
    private String storageType;

    @Autowired
    protected HttpServletRequest servletRequest;

    @Autowired
    private Environment environment;

    @Bean
    public StorageResource storageResource() {
        StorageType type = StorageType.getType(storageType);
        String directory = environment.getProperty("file.upload-dir");
        String serverUrl = environment.getProperty("system.backend.url");
        return new StorageLocal(new StorageNfsConfig(directory, serverUrl));
    }
}
