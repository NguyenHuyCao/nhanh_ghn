package com.app84soft.check_in.other_service.storage.nfs;

import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.other_service.storage.StorageConfig;
import com.app84soft.check_in.other_service.storage.StorageResource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log4j2
public class StorageLocal implements StorageResource {
    private final StorageNfsConfig config;

    public StorageLocal(StorageConfig config) {
        this.config = (StorageNfsConfig) config;
    }

    @Override
    public InputStream readResource(String path) {
        path = fixPath(path);
        String src = String.format("%s/%s", config.getDirectory(), path);
        try {
            return new FileInputStream(src);
        } catch (FileNotFoundException e) {
            log.error("File not found : {}", path);
            throw new BusinessException("File not found!", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public String writeResource(InputStream inputStream, String path) {
        path = fixPath(path);
        String src = String.format("%s/%s", config.getDirectory(), path);
        File file = new File(src);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.exists()) {
                    throw new IOException("Cannot create directory: " + parent);
                }
            }
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            log.error("Write file error : {}", src, e);
            throw new BusinessException("Write file error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return String.format("%s%s", config.getServerUrl(), path);
    }

    @Override
    public boolean deleteFile(String file) {
        file = fixPath(file);
        String src = String.format("%s/%s", config.getDirectory(), file);
        try {
            Path path = Paths.get(src);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.info("Can not remove temporary files");
            return false;
        }
        return true;
    }

    @Override
    public String getUrl(String file) {
        return config.getServerUrl() + fixPath(file);
    }

    private String fixPath(String path) {
        if (path == null)
            return "";
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
