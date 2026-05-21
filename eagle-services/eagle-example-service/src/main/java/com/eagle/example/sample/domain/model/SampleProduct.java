package com.eagle.example.sample.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.example.sample.domain.event.ProductCreatedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

/**
 * 商品聚合根。
 *
 * <p>演示特性：
 * <ul>
 *   <li>继承 {@link BaseAggregateRoot} — 审计字段 + 乐观锁 + 领域事件</li>
 *   <li>{@code @Convert} — 字段级加密（eagle-encrypt-starter）</li>
 *   <li>{@code @Comment} — JPA/Hibernate 字段注释</li>
 * </ul>
 */
@Entity
@Table(name = "sample_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleProduct extends BaseAggregateRoot<SampleProduct> {

    @Column(nullable = false, length = 128)
    @Comment("商品名称")
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    @Comment("商品价格")
    private BigDecimal price;

    @Column(nullable = false)
    @Comment("库存数量")
    private Integer stock;

    @Column(length = 512)
    @Comment("商品描述")
    private String description;

    @Column(name = "supplier_phone", length = 32)
    @Convert(converter = com.eagle.encrypt.converter.EncryptedStringConverter.class)
    @Comment("供应商电话（加密存储）")
    private String supplierPhone;

    @Column(nullable = false)
    @Comment("是否上架")
    private Boolean enabled = true;

    /**
     * 工厂方法：创建商品。
     */
    public static SampleProduct create(String name, BigDecimal price, Integer stock,
                                       String description, String supplierPhone) {
        SampleProduct product = new SampleProduct();
        product.name = name;
        product.price = price;
        product.stock = stock;
        product.description = description;
        product.supplierPhone = supplierPhone;
        product.enabled = true;
        product.registerEvent(new ProductCreatedEvent(null, name));
        return product;
    }

    /**
     * 扣减库存。
     */
    public void deductStock(int quantity) {
        if (this.stock < quantity) {
            throw com.eagle.example.sample.domain.SampleErrorCode.PRODUCT_STOCK_INSUFFICIENT.toDomainException();
        }
        this.stock -= quantity;
    }

    /**
     * 上架。
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 下架。
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * 更新商品信息。
     */
    public void update(String name, BigDecimal price, String description, String supplierPhone) {
        this.name = name;
        this.price = price;
        this.description = description;
        if (supplierPhone != null) {
            this.supplierPhone = supplierPhone;
        }
    }
}
