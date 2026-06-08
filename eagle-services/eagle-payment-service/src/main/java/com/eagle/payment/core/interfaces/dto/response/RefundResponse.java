package com.eagle.payment.core.interfaces.dto.response;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单详情响应。
 *
 * @author sunshixiong
 */
@Schema(description = "退款单详情")
public record RefundResponse(
        @Schema(description = "退款单 ID", example = "9527") Long id,
        @Schema(description = "关联 Payment ID", example = "1024") Long paymentId,
        @Schema(description = "业务退款号") String bizRefundNo,
        @Schema(description = "渠道") PaymentChannel channel,
        @Schema(description = "退款金额(元)") BigDecimal amount,
        @Schema(description = "退款原因") String reason,
        @Schema(description = "渠道退款单号") String channelRefundNo,
        @Schema(description = "状态") RefundStatus status,
        @Schema(description = "退款完成时间") LocalDateTime refundedAt,
        @Schema(description = "失败原因") String failReason,
        @Schema(description = "创建时间") LocalDateTime createTime
) {
}
