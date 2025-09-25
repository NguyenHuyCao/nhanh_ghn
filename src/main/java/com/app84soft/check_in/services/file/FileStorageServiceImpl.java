package com.app84soft.check_in.services.file;

import com.app84soft.check_in.entities.file.UploadFile;
import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.other_service.excel.ExcelConstant;
import com.app84soft.check_in.other_service.excel.ExcelService;
import com.app84soft.check_in.other_service.storage.StorageResource;
import com.app84soft.check_in.repositories.file.MediaRepository;
import com.app84soft.check_in.services.BaseService;
import com.app84soft.check_in.util.Constants;
import com.app84soft.check_in.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileStorageServiceImpl extends BaseService implements FileStorageService {
    @Autowired
    private StorageResource storageResource;

    @Autowired
    private MediaRepository mediaRepository;

    @Override
    public UploadFile storeExcel(MultipartFile file, Integer courseId) {
        String timeStamp = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_mm_SSS).format(new Date());
        String randomString = RandomStringUtils.random(6, Constants.ALPHA_NUM);
        String fileName = Util.removeCharacterVn(StringUtils.cleanPath(file.getOriginalFilename().toLowerCase()));
        String originalName = timeStamp + "_" + randomString + "_" + fileName;

        if (originalName.contains("..")) {
            throw new BusinessException("Sorry! Filename contains invalid path sequence " + originalName);
        }
        String type = file.getContentType();
        if (type == null || !type.toLowerCase().startsWith("application/vnd.ms-excel") && !type.toLowerCase().startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml") && !originalName.endsWith(".xlsx") && !originalName.endsWith(".xls")) {
            throw new BusinessException("File format error");
        }

        try {
            UploadFile excel = new UploadFile();
            excel.setOriginFilePath(String.format("excel/%s", originalName));
            InputStream inputData = rewritePhone(file);
            excel.setOriginUrl(storageResource.writeResource(inputData, "excel/" + originalName));
            excel.setSize(file.getSize());
            excel.setName(originalName);
            mediaRepository.save(excel);
            return excel;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(e.getMessage());
        }
    }

    private InputStream rewritePhone(MultipartFile file) {
        try {
            InputStream inputData = file.getInputStream();
            Workbook workbook;
            try {
                if (file.getContentType() != null && file.getContentType().equals("application/vnd.ms-excel")) {
                    workbook = new HSSFWorkbook(inputData);
                } else if (file.getContentType() != null && file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                    workbook = new XSSFWorkbook(inputData);
                } else {
                    throw new BusinessException("Loại file không được hỗ trợ");
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new BusinessException("Sai loại file");
            }

            Sheet sheet = workbook.getSheetAt(0);
            int rows = sheet.getLastRowNum();
//            int cols = sheet.getRow(1).getLastCellNum();

            for (int i = 2; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Cell cell = row.getCell(ExcelConstant.phoneCol);
                if (cell == null) {
                    continue;
                }
                String phone = ExcelService.getCellValue(cell);
                if (phone.isBlank()) {
                    continue;
                }
                phone = phone.replaceAll("\\D", "");
                if (phone.startsWith("84")) {
                    phone = phone.substring(2);
                    phone = "0" + phone;
                } else if (!phone.startsWith("0")) {
                    phone = "0" + phone;
                }
                cell.setCellValue(phone);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException("Failed to process Excel file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void deleteFile(int fileId) {
        UploadFile uploadFile = mediaRepository.findUploadFileById(fileId);
        if (uploadFile != null) {
            String fileName = uploadFile.getOriginFilePath();
            if (StringUtils.hasText(fileName)) {
                storageResource.deleteFile(fileName);
            }
            mediaRepository.delete(uploadFile);
        }
    }

    @Override
    public InputStream getInputStream(String fileName) {
        return storageResource.readResource(fileName);
    }

    @Override
    public InputStream getInputStream(Integer fileId) {
        mediaRepository.findUploadFileById(fileId);
        String fileName = mediaRepository.findUploadFileById(fileId).getOriginFilePath();
        if (StringUtils.hasText(fileName)) {
            return storageResource.readResource(fileName);
        } else {
            throw new BusinessException("File not found");
        }
    }

    @Override
    public UploadFile getDetail(Integer uploadFileId) {
        return mediaRepository.findUploadFileById(uploadFileId);
    }
}
