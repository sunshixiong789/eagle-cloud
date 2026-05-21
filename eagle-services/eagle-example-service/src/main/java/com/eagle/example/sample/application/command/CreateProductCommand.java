package com.eagle.example.sample.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建商品命令。
 */
public record CreateProductCommand(
        @NotBlank @Size(max = 128) String name,
        @DecimalMin("0.01") BigDecimal price,
        @Min(0) Integer stock,
        @Size(max = 512) String description,
        @Size(max = 32) String supplierPhone
) {
}
