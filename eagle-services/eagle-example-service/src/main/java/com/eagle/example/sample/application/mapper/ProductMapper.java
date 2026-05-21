package com.eagle.example.sample.application.mapper;

import com.eagle.example.sample.application.command.CreateProductCommand;
import com.eagle.example.sample.application.dto.ProductDto;
import com.eagle.example.sample.domain.model.SampleProduct;
import org.springframework.stereotype.Component;

/**
 * 商品 DTO ↔ 领域对象映射器。
 */
@Component
public class ProductMapper {

    public SampleProduct toEntity(CreateProductCommand command) {
        return SampleProduct.create(
                command.name(),
                command.price(),
                command.stock(),
                command.description(),
                command.supplierPhone()
        );
    }

    public ProductDto toDto(SampleProduct product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getEnabled(),
                product.getCreateTime()
        );
    }
}
