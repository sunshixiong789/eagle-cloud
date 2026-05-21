package com.eagle.example.integration.message;

import com.eagle.example.sample.domain.event.ProductCreatedEvent;
import com.eagle.rocketmq.listener.AbstractRocketMqListener;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ Starter 验证：商品创建事件消费者。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "eagle.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleProductEventConsumer extends AbstractRocketMqListener<ProductCreatedEvent> {

    public SampleProductEventConsumer(RocketMqProperties rocketMqProperties) {
        super(rocketMqProperties);
    }

    @Override
    protected String getTopic() {
        return "example_product_created";
    }

    @Override
    protected Class<ProductCreatedEvent> getEventClass() {
        return ProductCreatedEvent.class;
    }

    @Override
    protected void handle(ProductCreatedEvent event) {
        log.info("[RocketMQ] ProductCreatedEvent consumed: productId={}, productName={}",
                event.getProductId(), event.getProductName());
    }
}
