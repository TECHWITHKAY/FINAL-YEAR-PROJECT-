package com.ghana.commoditymonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghana.commoditymonitor.dto.request.ExportRequestDto;
import com.ghana.commoditymonitor.dto.response.ExportLogDto;
import com.ghana.commoditymonitor.entity.ExportLog;
import com.ghana.commoditymonitor.entity.PriceRecord;
import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.enums.ExportType;
import com.ghana.commoditymonitor.exception.BusinessRuleException;
import com.ghana.commoditymonitor.repository.ExportLogRepository;
import com.ghana.commoditymonitor.repository.PriceRecordRepository;
import com.ghana.commoditymonitor.repository.UserRepository;
import com.ghana.commoditymonitor.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataExportService {

    private final PriceRecordRepository priceRecordRepository;
    private final ExportLogRepository exportLogRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    private static final int MAX_EXPORT_ROWS = 100_000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    public record ExportResult(byte[] data, String filename, String contentType, int rowCount) {}

    @Transactional
    public ExportResult exportPriceRecords(ExportRequestDto dto, UserPrincipal principal, String ipAddress) {
        log.info("Exporting price records: type={}, user={}", dto.exportType(), principal.username());

        if (dto.toDate() != null && dto.fromDate() != null && dto.toDate().isBefore(dto.fromDate())) {
            throw new BusinessRuleException("End date cannot be before start date");
        }

        List<PriceRecord> records = fetchFilteredRecords(dto);

        if (records.size() > MAX_EXPORT_ROWS) {
            throw new BusinessRuleException(
                String.format("Export exceeds %,d rows. Please apply filters.", MAX_EXPORT_ROWS)
            );
        }

        byte[] data;
        String filename;
        String contentType;

        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (dto.exportType() == ExportType.CSV) {
            data = generateCsv(records);
            filename = "commodity_prices_" + timestamp + ".csv";
            contentType = "text/csv; charset=UTF-8";
        } else {
            data = generateExcel(records, dto.includeAnalyticsSummary());
            filename = "commodity_prices_" + timestamp + ".xlsx";
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }

        saveExportLog(dto, principal, ipAddress, records.size(), data.length);

        log.info("Export completed: {} rows, {} bytes", records.size(), data.length);
        return new ExportResult(data, filename, contentType, records.size());
    }

    private List<PriceRecord> fetchFilteredRecords(ExportRequestDto dto) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRecord> query = cb.createQuery(PriceRecord.class);
        Root<PriceRecord> root = query.from(PriceRecord.class);

        root.fetch("commodity", JoinType.LEFT);
        root.fetch("market", JoinType.LEFT);
        root.fetch("submittedBy", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("status"), com.ghana.commoditymonitor.enums.PriceRecordStatus.APPROVED));

        if (dto.commodityId() != null) {
            predicates.add(cb.equal(root.get("commodity").get("id"), dto.commodityId()));
        }

        if (dto.marketId() != null) {
            predicates.add(cb.equal(root.get("market").get("id"), dto.marketId()));
        }

        if (dto.cityId() != null) {
            predicates.add(cb.equal(root.get("market").get("city").get("id"), dto.cityId()));
        }

        if (dto.fromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("recordedDate"), dto.fromDate()));
        }

        if (dto.toDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("recordedDate"), dto.toDate()));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(
            cb.desc(root.get("recordedDate")),
            cb.asc(root.get("commodity").get("name"))
        );

        TypedQuery<PriceRecord> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

    private byte[] generateCsv(List<PriceRecord> records) {
        StringBuilder csv = new StringBuilder();

        csv.append("ID,Commodity,Category,Unit,Market,City,Price (GHS),Recorded Date,Source,Submitted By,Status\n");

        for (PriceRecord record : records) {
            csv.append(record.getId()).append(",");
            csv.append(escapeCsv(record.getCommodity().getName())).append(",");
            csv.append(escapeCsv(record.getCommodity().getCategory())).append(",");
            csv.append(escapeCsv(record.getCommodity().getUnit())).append(",");
            csv.append(escapeCsv(record.getMarket().getName())).append(",");
            csv.append(escapeCsv(record.getMarket().getCity().getName())).append(",");
            csv.append(record.getPrice()).append(",");
            csv.append(record.getRecordedDate().format(DATE_FORMATTER)).append(",");
            csv.append(escapeCsv(record.getSource() != null ? record.getSource() : "")).append(",");
            csv.append(escapeCsv(record.getSubmittedBy() != null ? record.getSubmittedBy().getUsername() : "")).append(",");
            csv.append(record.getStatus()).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private byte[] generateExcel(List<PriceRecord> records, boolean includeAnalytics) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            createPriceRecordsSheet(workbook, records);

            if (includeAnalytics) {
                createMonthlySummarySheet(workbook, records);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generating Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private void createPriceRecordsSheet(XSSFWorkbook workbook, List<PriceRecord> records) {
        Sheet sheet = workbook.createSheet("Price Records");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle evenRowStyle = createEvenRowStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Commodity", "Category", "Unit", "Market", "City", 
                           "Price (GHS)", "Recorded Date", "Source", "Status"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (PriceRecord record : records) {
            Row row = sheet.createRow(rowNum);

            if (rowNum % 2 == 0) {
                for (int i = 0; i < headers.length; i++) {
                    row.createCell(i).setCellStyle(evenRowStyle);
                }
            }

            row.getCell(0).setCellValue(record.getId());
            row.getCell(1).setCellValue(record.getCommodity().getName());
            row.getCell(2).setCellValue(record.getCommodity().getCategory());
            row.getCell(3).setCellValue(record.getCommodity().getUnit());
            row.getCell(4).setCellValue(record.getMarket().getName());
            row.getCell(5).setCellValue(record.getMarket().getCity().getName());

            Cell priceCell = row.getCell(6);
            priceCell.setCellValue(record.getPrice().doubleValue());
            priceCell.setCellStyle(currencyStyle);

            Cell dateCell = row.getCell(7);
            dateCell.setCellValue(record.getRecordedDate());
            dateCell.setCellStyle(dateStyle);

            row.getCell(8).setCellValue(record.getSource() != null ? record.getSource() : "");
            row.getCell(9).setCellValue(record.getStatus().name());

            rowNum++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 50 * 256) {
                sheet.setColumnWidth(i, 50 * 256);
            }
        }

        sheet.createFreezePane(0, 1);
    }

    private void createMonthlySummarySheet(XSSFWorkbook workbook, List<PriceRecord> records) {
        Sheet sheet = workbook.createSheet("Monthly Summary");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        Map<String, Map<String, List<BigDecimal>>> monthlyData = records.stream()
            .collect(Collectors.groupingBy(
                pr -> pr.getCommodity().getName(),
                Collectors.groupingBy(
                    pr -> pr.getRecordedDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    Collectors.mapping(PriceRecord::getPrice, Collectors.toList())
                )
            ));

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Commodity", "Month", "Average Price (GHS)", "Record Count"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Map.Entry<String, Map<String, List<BigDecimal>>> commodityEntry : monthlyData.entrySet()) {
            String commodity = commodityEntry.getKey();

            for (Map.Entry<String, List<BigDecimal>> monthEntry : commodityEntry.getValue().entrySet()) {
                String month = monthEntry.getKey();
                List<BigDecimal> prices = monthEntry.getValue();

                BigDecimal avgPrice = prices.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(prices.size()), 2, java.math.RoundingMode.HALF_UP);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(commodity);
                row.createCell(1).setCellValue(month);

                Cell avgCell = row.createCell(2);
                avgCell.setCellValue(avgPrice.doubleValue());
                avgCell.setCellStyle(currencyStyle);

                row.createCell(3).setCellValue(prices.size());
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 50 * 256) {
                sheet.setColumnWidth(i, 50 * 256);
            }
        }

        sheet.createFreezePane(0, 1);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createEvenRowStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd-mmm-yyyy"));
        return style;
    }

    @Transactional
    private void saveExportLog(ExportRequestDto dto, UserPrincipal principal, 
                               String ipAddress, int rowCount, long fileSize) {
        try {
            User user = userRepository.findByUsername(principal.username()).orElse(null);

            String filtersJson = objectMapper.writeValueAsString(Map.of(
                "commodityId", dto.commodityId() != null ? dto.commodityId() : "null",
                "marketId", dto.marketId() != null ? dto.marketId() : "null",
                "cityId", dto.cityId() != null ? dto.cityId() : "null",
                "fromDate", dto.fromDate() != null ? dto.fromDate().toString() : "null",
                "toDate", dto.toDate() != null ? dto.toDate().toString() : "null",
                "includeAnalyticsSummary", dto.includeAnalyticsSummary()
            ));

            ExportLog log = ExportLog.builder()
                .user(user)
                .exportType(dto.exportType().name())
                .filters(filtersJson)
                .rowCount(rowCount)
                .fileSize(fileSize)
                .ipAddress(ipAddress)
                .build();

            exportLogRepository.save(log);

        } catch (Exception e) {
            log.error("Failed to save export log", e);
        }
    }

    public Page<ExportLogDto> getMyExportHistory(Long userId, Pageable pageable) {
        return exportLogRepository.findByUserIdOrderByExportedAtDesc(userId, pageable)
            .map(this::mapToDto);
    }

    public Page<ExportLogDto> getAllExportHistory(Pageable pageable) {
        return exportLogRepository.findAllByOrderByExportedAtDesc(pageable)
            .map(this::mapToDto);
    }

    private ExportLogDto mapToDto(ExportLog log) {
        return new ExportLogDto(
            log.getId(),
            log.getUser() != null ? log.getUser().getUsername() : null,
            log.getExportType(),
            log.getRowCount(),
            log.getFileSize() / 1024,
            log.getIpAddress(),
            log.getExportedAt()
        );
    }
}
