package com.website.util;

import com.website.entity.Company;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ExcelParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Company> parseExcel(MultipartFile file) throws IOException {
        log.info("开始解析Excel文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        List<Company> companies = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            log.info("Excel文件打开成功，工作表数: {}", workbook.getNumberOfSheets());

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Company company = new Company();
                company.setSerialNumber(i);
                company.setCompanyName(getCellValue(row, headerRow, "公司名称"));
                company.setEmail(getCellValue(row, headerRow, "邮箱"));
                // 获取域名并确保以.cn结尾
                String domain = getCellValue(row, headerRow, "域名");
                if (domain != null && !domain.trim().isEmpty()) {
                    domain = domain.trim();
                    String originalDomain = domain;
                    // 如果域名不以.cn结尾，则添加.cn后缀
                    if (!domain.toLowerCase().endsWith(".cn")) {
                        domain = domain + ".cn";
                        log.info("第{}行: 域名 '{}' 自动添加.cn后缀 => '{}'", i+1, originalDomain, domain);
                    } else {
                        log.info("第{}行: 域名 '{}' 已包含.cn后缀", i+1, domain);
                    }
                    company.setDomain(domain);
                } else {
                    log.info("第{}行: 域名为空", i+1);
                    company.setDomain(null);
                }
                company.setIcpNumber(getCellValue(row, headerRow, "备案号"));

                // 检查域名是否已存在
                if (company.getDomain() != null && !company.getDomain().isEmpty()) {
                    // 可以在这里添加重复检查逻辑
                }

                companies.add(company);
            }
        }

        log.info("Excel解析完成，共解析 {} 条公司记录", companies.size());
        return companies;
    }

    private String getCellValue(Row row, Row headerRow, String columnName) {
        if (headerRow == null || row == null) {
            return null;
        }

        int columnIdx = findColumnIndex(headerRow, columnName);
        if (columnIdx == -1) {
            log.warn("未找到列 '{}'，返回null", columnName);
            return null;
        }

        Cell cell = row.getCell(columnIdx);
        if (cell == null) {
            return null;
        }

        return getCellValueAsString(cell);
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        if (headerRow == null) {
            log.debug("headerRow is null, cannot find column: {}", columnName);
            return -1;
        }

        for (Cell cell : headerRow) {
            String cellValue = getCellValueAsString(cell);
            if (cellValue != null && cellValue.trim().equalsIgnoreCase(columnName.trim())) {
                log.debug("找到列 '{}' 在索引 {}", columnName, cell.getColumnIndex());
                return cell.getColumnIndex();
            }
        }
        log.warn("未找到列 '{}'，表头可能不匹配", columnName);
        return -1;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                            .format(DATE_FORMATTER);
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试直接解析
            return LocalDateTime.parse(dateTimeStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                // 尝试只解析日期
                String[] parts = dateTimeStr.split(" ");
                if (parts.length > 0) {
                    return LocalDateTime.parse(parts[0] + " 00:00:00", DATE_FORMATTER);
                }
            } catch (Exception ex) {
                // 忽略错误
            }
        }

        return null;
    }
}
