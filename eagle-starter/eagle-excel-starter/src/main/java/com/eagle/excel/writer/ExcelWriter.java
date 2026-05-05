package com.eagle.excel.writer;

import com.eagle.excel.annotation.ExcelColumn;
import com.eagle.excel.properties.ExcelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 泛型 Excel 导出工具。
 *
 * <p>支持字段类型：{@code String / Integer / Long / Double / BigDecimal / Boolean /
 * LocalDate / LocalDateTime}，以及所有 {@code toString()} 有意义的类型。
 *
 * <p>数据量超过 {@code eagle.excel.streaming-threshold}（默认 10000）时自动切换
 * SXSSF 流式写入，内存仅保留 100 行窗口。
 *
 * <p>使用示例：
 * <pre>
 * // 返回字节数组（推荐 Controller 直接写响应）
 * byte[] bytes = excelWriter.write(orders, OrderExportDto.class);
 * response.getOutputStream().write(bytes);
 *
 * // 或直接写入 OutputStream
 * excelWriter.writeTo(orders, OrderExportDto.class, response.getOutputStream());
 * </pre>
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class ExcelWriter {

    private final ExcelProperties properties;

    /**
     * 将数据导出为 Excel 字节数组（.xlsx 格式）。
     */
    public <T> byte[] write(List<T> data, Class<T> clazz) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTo(data, clazz, out);
        return out.toByteArray();
    }

    /**
     * 将数据导出并写入 OutputStream（用于 HTTP 响应直传）。
     */
    public <T> void writeTo(List<T> data, Class<T> clazz, OutputStream outputStream) throws IOException {
        List<Field> columns = resolveColumns(clazz);
        boolean streaming = data.size() > properties.getStreamingThreshold();

        try (Workbook workbook = streaming ? new SXSSFWorkbook(100) : new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(properties.getDefaultSheetName());
            writeHeader(sheet, columns, workbook);
            writeRows(sheet, data, columns);
            workbook.write(outputStream);
        }

        log.debug("Excel export: class={}, rows={}, streaming={}",
                clazz.getSimpleName(), data.size(), streaming);
    }

    private List<Field> resolveColumns(Class<?> clazz) {
        List<Field> all = new ArrayList<>();
        collectFields(clazz, all);

        return all.stream()
                .filter(f -> f.isAnnotationPresent(ExcelColumn.class))
                .peek(f -> f.setAccessible(true))
                .sorted(Comparator.comparingInt(f -> {
                    int idx = f.getAnnotation(ExcelColumn.class).index();
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .toList();
    }

    private void collectFields(Class<?> clazz, List<Field> result) {
        result.addAll(Arrays.asList(clazz.getDeclaredFields()));
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            collectFields(clazz.getSuperclass(), result);
        }
    }

    private void writeHeader(Sheet sheet, List<Field> columns, Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i).getAnnotation(ExcelColumn.class).value());
            cell.setCellStyle(style);
        }
    }

    private <T> void writeRows(Sheet sheet, List<T> data, List<Field> columns) {
        for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
            Row row = sheet.createRow(rowIdx + 1);
            T item = data.get(rowIdx);
            for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                Cell cell = row.createCell(colIdx);
                try {
                    setCellValue(cell, columns.get(colIdx), item);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            "无法读取字段 " + columns.get(colIdx).getName() + " 的值", e);
                }
            }
        }
    }

    private void setCellValue(Cell cell, Field field, Object obj) throws IllegalAccessException {
        Object value = field.get(obj);
        if (value == null) {
            return;
        }

        ExcelColumn col = field.getAnnotation(ExcelColumn.class);

        if (value instanceof String s) {
            cell.setCellValue(s);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (value instanceof LocalDate ld) {
            String fmt = StringUtils.hasText(col.dateFormat()) ? col.dateFormat() : properties.getDateFormat();
            cell.setCellValue(ld.format(DateTimeFormatter.ofPattern(fmt)));
        } else if (value instanceof LocalDateTime ldt) {
            String fmt = StringUtils.hasText(col.dateFormat()) ? col.dateFormat() : properties.getDatetimeFormat();
            cell.setCellValue(ldt.format(DateTimeFormatter.ofPattern(fmt)));
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
