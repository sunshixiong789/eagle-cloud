package com.eagle.excel.reader;

import com.eagle.excel.annotation.ExcelColumn;
import com.eagle.excel.properties.ExcelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 泛型 Excel 导入工具。
 *
 * <p>通过 {@link ExcelColumn} 注解驱动列与字段的映射，支持以下字段类型：
 * {@code String / Integer / Long / Double / BigDecimal / Boolean / LocalDate / LocalDateTime}。
 *
 * <p>使用示例：
 * <pre>
 * List&lt;OrderImportDto&gt; orders = excelReader.read(multipartFile.getInputStream(), OrderImportDto.class);
 * </pre>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelReader {

    private final ExcelProperties properties;

    /**
     * 从 InputStream 读取 Excel 并映射为目标类型列表（读取第一个 Sheet）。
     *
     * @param inputStream Excel 输入流（.xlsx / .xls 均支持）
     * @param targetClass 目标 DTO 类，字段需标注 {@link ExcelColumn}
     * @param <T>         目标类型
     * @return 映射后的对象列表，空行自动跳过
     * @throws IOException 读取失败时抛出
     */
    public <T> List<T> read(InputStream inputStream, Class<T> targetClass) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return readSheet(workbook.getSheetAt(0), targetClass);
        }
    }

    /**
     * 读取指定 Sheet 索引（0-based）。
     */
    public <T> List<T> read(InputStream inputStream, Class<T> targetClass, int sheetIndex) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            return readSheet(workbook.getSheetAt(sheetIndex), targetClass);
        }
    }

    private <T> List<T> readSheet(Sheet sheet, Class<T> clazz) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum > properties.getMaxRows()) {
            throw new IllegalArgumentException(
                    "Excel 行数 " + lastRowNum + " 超过最大限制 " + properties.getMaxRows());
        }

        Row headerRow = sheet.getRow(0);
        Map<Integer, Field> columnMapping = buildColumnMapping(clazz, headerRow);

        List<T> result = new ArrayList<>(lastRowNum);
        for (int i = 1; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            if (isBlankRow(row)) {
                continue;
            }
            result.add(mapRowToObject(row, columnMapping, clazz));
        }
        log.debug("Excel import: sheet={}, total={}", sheet.getSheetName(), result.size());
        return result;
    }

    private <T> Map<Integer, Field> buildColumnMapping(Class<T> clazz, Row headerRow) {
        // 收集所有带注解的字段：先按标题名分组，再按 index 分组
        Map<String, Field> titleToField = new HashMap<>();
        Map<Integer, Field> indexToField = new HashMap<>();

        getAllFields(clazz).stream()
                .filter(f -> f.isAnnotationPresent(ExcelColumn.class))
                .forEach(f -> {
                    f.setAccessible(true);
                    ExcelColumn col = f.getAnnotation(ExcelColumn.class);
                    if (col.index() >= 0) {
                        indexToField.put(col.index(), f);
                    } else {
                        titleToField.put(col.value().trim(), f);
                    }
                });

        // 用表头行补充标题→序号映射
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                String title = getStringValue(cell).trim();
                Field field = titleToField.get(title);
                if (field != null) {
                    indexToField.putIfAbsent(cell.getColumnIndex(), field);
                }
            }
        }
        return indexToField;
    }

    private <T> T mapRowToObject(Row row, Map<Integer, Field> mapping, Class<T> clazz) {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<Integer, Field> entry : mapping.entrySet()) {
                Cell cell = row.getCell(entry.getKey());
                Field field = entry.getValue();
                ExcelColumn col = field.getAnnotation(ExcelColumn.class);

                if (cell == null || cell.getCellType() == CellType.BLANK) {
                    if (col.required()) {
                        throw new IllegalArgumentException(
                                "第 " + (row.getRowNum() + 1) + " 行，列 [" + col.value() + "] 不能为空");
                    }
                    continue;
                }
                setFieldValue(obj, field, cell);
            }
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("无法映射第 " + (row.getRowNum() + 1) + " 行到 " + clazz.getSimpleName(), e);
        }
    }

    private void setFieldValue(Object obj, Field field, Cell cell) throws IllegalAccessException {
        ExcelColumn col = field.getAnnotation(ExcelColumn.class);
        Class<?> type = field.getType();

        if (type == String.class) {
            field.set(obj, getStringValue(cell));
        } else if (type == Integer.class || type == int.class) {
            field.set(obj, (int) cell.getNumericCellValue());
        } else if (type == Long.class || type == long.class) {
            field.set(obj, (long) cell.getNumericCellValue());
        } else if (type == Double.class || type == double.class) {
            field.set(obj, cell.getNumericCellValue());
        } else if (type == BigDecimal.class) {
            field.set(obj, BigDecimal.valueOf(cell.getNumericCellValue()));
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(obj, resolveBooleanValue(cell));
        } else if (type == LocalDate.class) {
            String fmt = StringUtils.hasText(col.dateFormat()) ? col.dateFormat() : properties.getDateFormat();
            field.set(obj, LocalDate.parse(getStringValue(cell), DateTimeFormatter.ofPattern(fmt)));
        } else if (type == LocalDateTime.class) {
            if (DateUtil.isCellDateFormatted(cell)) {
                field.set(obj, cell.getLocalDateTimeCellValue());
            } else {
                String fmt = StringUtils.hasText(col.dateFormat()) ? col.dateFormat() : properties.getDatetimeFormat();
                field.set(obj, LocalDateTime.parse(getStringValue(cell), DateTimeFormatter.ofPattern(fmt)));
            }
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern(properties.getDatetimeFormat()))
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private boolean resolveBooleanValue(Cell cell) {
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        String v = getStringValue(cell).toLowerCase();
        return "1".equals(v) || "true".equals(v) || "是".equals(v) || "yes".equals(v);
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            fields.addAll(getAllFields(clazz.getSuperclass()));
        }
        return fields;
    }
}
