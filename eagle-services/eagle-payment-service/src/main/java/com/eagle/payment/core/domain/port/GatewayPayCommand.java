package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交支付订单到渠道的入参 (渠道无关)。
 *
 * <p>{@link #outTradeNo} 是支付服务自己生成的全局唯一交易号 (snowflake / 业务编号),
 * 作为渠道侧的"商户订单号"。渠道返回的交易号会通过 {@link GatewayPayResult#channelTradeNo}
 * 回填到 Payment 聚合根的 outTradeNo 字段。
 *
 * @author sunshixiong
 */
public record GatewayPayCommand(
        PaymentChannel channel,
        PaymentScene scene,
        String outTradeNo,
        BigDecimal amount,
        String currency,
        String subject,
        LocalDateTime expiresAt,
        @Nullable String clientIp,
        @Nullable String returnUrl,
        @Nullable String notifyUrl,
        @Nullable String openId
) {
}
