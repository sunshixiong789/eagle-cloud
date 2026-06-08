package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 渠道清算单条目 (统一抽象,不绑定具体渠道字段)。
 *
 * @author sunshixiong
 */
public record BillEntry(
        PaymentChannel channel,
        String outTradeNo,
        @Nullable String channelTradeNo,
        BigDecimal amount,
        String channelStatus
) {
}
