package com.eagle.example.sample.domain.event;

import com.eagle.common.event.BaseEvent;
import lombok.Getter;

/**
 * 商品创建领域事件。
 */
@Getter
public class ProductCreatedEvent extends BaseEvent {

    private final Long productId;
    private final String productName;

    public ProductCreatedEvent(Long productId, String productName) {
        this.productId = productId;
        this.productName = productName;
    }
}
