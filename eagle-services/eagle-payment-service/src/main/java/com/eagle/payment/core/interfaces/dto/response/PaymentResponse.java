package com.eagle.payment.core.interfaces.dto.response;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单详情响应。
 *
 * @author sunshixiong
 */
@Schema(description = "支付订单详情")
public record PaymentResponse(
        @Schema(description = "支付订单 ID", example = "1024") Long id,
        @Schema(description = "业务订单号", example = "ORD20260608001") String bizOrderNo,
        @Schema(description = "渠道") PaymentChannel channel,
        @Schema(description = "场景") PaymentScene scene,
        @Schema(description = "金额(元)") BigDecimal amount,
        @Schema(description = "币种") String currency,
        @Schema(description = "订单标题") String subject,
        @Schema(description = "渠道交易号") String outTradeNo,
        @Schema(description = "状态") PaymentStatus status,
        @Schema(description = "已退款金额(元)") BigDecimal refundedAmount,
        @Schema(description = "支付完成时间") LocalDateTime paidAt,
        @Schema(description = "过期时间") LocalDateTime expiresAt,
        @Schema(description = "失败原因") String failReason,
        @Schema(description = "创建时间") LocalDateTime createTime
) {
}
