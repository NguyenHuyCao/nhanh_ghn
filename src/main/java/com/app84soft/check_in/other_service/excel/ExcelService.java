package com.app84soft.check_in.other_service.excel;

import com.app84soft.check_in.exceptions.BusinessException;
import com.app84soft.check_in.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Slf4j
public class ExcelService {
    public static String getCellValue(Cell cell) {
        try {
            if (cell == null) {
                return "";
            }
            switch (cell.getCellType()) {
                case STRING -> {
                    return cell.getStringCellValue();
                }
                case FORMULA -> {
                    return cell.getCellFormula();
                }
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        return Util.convertDateToString(date, "dd/MM/yyyy");
                    } else {
                        double value = cell.getNumericCellValue();
                        return String.format("%.0f", value);
                    }
                }
                case BOOLEAN -> {
                    return Boolean.toString(cell.getBooleanCellValue());
                }
                default -> {
                    return "";
                }
            }
        } catch (Exception e) {
            log.info("getCellValue: {}", e.getMessage());
            return "";
        }
    }

    public static void createCell(Row row, String value, int index, CellStyle style) {
        if (value == null) {
            value = "";
        }
        Cell cell;
        try {
            double v = Double.parseDouble(value);
            cell = row.createCell(index, CellType.NUMERIC);
            cell.setCellValue(v);
        } catch (Exception e) {
            cell = row.createCell(index, CellType.STRING);
            cell.setCellValue(value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
    public static void createCell(Row row, String value, int index, CellStyle style, CellType type) {
        if (value == null) {
            value = "";
        }
        Cell cell;
        try {
            cell = row.createCell(index, type);
            cell.setCellValue(value);
        } catch (Exception e) {
            log.info(e.getMessage());
           throw new BusinessException("Error in creating cell", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    public static CellStyle createStyleForTitle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }
}
