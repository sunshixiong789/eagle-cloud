package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.RefundStatus;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 渠道退款返回。
 *
 * <p>渠道行为差异:
 * <ul>
 *   <li>支付宝 alipay.trade.refund - 同步即返回退款最终结果 (REFUNDED / FAILED);
 *       refundedAt 取响应时间</li>
 *   <li>微信 V3 refund - 同步返回 PROCESSING / SUCCESS;少数情况通过异步通知到达</li>
 * </ul>
 *
 * <p>{@link #channelRefundNo} 是渠道侧自有退款单号 (支付宝 trade_no / 微信 refund_id),
 * 异步回调时使用此号匹配 Refund。
 *
 * @author sunshixiong
 */
public record GatewayRefundResult(
        @Nullable String channelRefundNo,
        RefundStatus status,
        @Nullable LocalDateTime refundedAt,
        @Nullable String failReason
) {
}
