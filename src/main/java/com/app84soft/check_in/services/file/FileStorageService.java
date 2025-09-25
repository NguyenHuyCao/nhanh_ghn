package com.app84soft.check_in.services.file;

import com.app84soft.check_in.entities.file.UploadFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    UploadFile storeExcel(final MultipartFile file, Integer courseId);
    void deleteFile(int fileId);
    InputStream getInputStream(final String fileName);
    InputStream getInputStream(Integer uploadFileId);
    UploadFile getDetail(Integer uploadFileId);

}
