package com.eagle.example.sample.application.dto;

import com.eagle.excel.annotation.ExcelColumn;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 DTO（含 Excel 导出注解）。
 */
public record ProductDto(
        Long id,

        @ExcelColumn("商品名称")
        String name,

        @ExcelColumn("价格")
        BigDecimal price,

        @ExcelColumn("库存")
        Integer stock,

        @ExcelColumn("描述")
        String description,

        @ExcelColumn("是否上架")
        Boolean enabled,

        @ExcelColumn("创建时间")
        LocalDateTime createTime
) {
}
