package com.eagle.example.sample.application.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新商品命令。
 */
public record UpdateProductCommand(
        @NotNull Long id,
        @Size(max = 128) String name,
        @DecimalMin("0.01") BigDecimal price,
        @Size(max = 512) String description,
        @Size(max = 32) String supplierPhone
) {
}
