package com.eagle.excel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel 列映射注解，标注在 DTO 字段上，驱动 {@link com.eagle.excel.reader.ExcelReader} 和
 * {@link com.eagle.excel.writer.ExcelWriter} 的列识别与类型转换。
 *
 * <p>使用示例：
 * <pre>
 * public class OrderImportDto {
 *
 *     &#64;ExcelColumn(value = "订单号", index = 0, required = true)
 *     private String orderNo;
 *
 *     &#64;ExcelColumn(value = "下单时间", index = 1, dateFormat = "yyyy/MM/dd HH:mm")
 *     private LocalDateTime orderTime;
 *
 *     &#64;ExcelColumn(value = "金额", index = 2)
 *     private BigDecimal amount;
 * }
 * </pre>
 *
 * @author eagle
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * 列标题（导出时作为表头，导入时用于列标题匹配）。
     */
    String value();

    /**
     * 列序号（0-based）。
     * <ul>
     *   <li>导出时按此顺序排列列；-1 表示按字段声明顺序</li>
     *   <li>导入时若标题匹配失败，则按此序号作为后备定位</li>
     * </ul>
     */
    int index() default -1;

    /**
     * 日期/时间格式，仅对 {@code Date / LocalDate / LocalDateTime} 字段有效。
     * 空字符串表示使用 {@code eagle.excel.date-format} / {@code eagle.excel.datetime-format} 全局配置。
     */
    String dateFormat() default "";

    /**
     * 导入时是否必填。若单元格为空且此值为 {@code true}，读取时抛出 {@code ExcelImportException}。
     */
    boolean required() default false;
}
