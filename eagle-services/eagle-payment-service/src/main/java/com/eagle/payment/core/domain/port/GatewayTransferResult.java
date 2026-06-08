package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.TransferStatus;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 渠道转账返回。
 *
 * <p>渠道差异:
 * <ul>
 *   <li>支付宝 fund.trans.uni.transfer - 同步即返回最终结果 (SUCCESS / FAILED)</li>
 *   <li>微信商家转账 - 同步返回 PROCESSING,最终结果通过查询 / 异步通知确认</li>
 * </ul>
 *
 * @author sunshixiong
 */
public record GatewayTransferResult(
        @Nullable String channelTransferNo,
        TransferStatus status,
        @Nullable LocalDateTime succeededAt,
        @Nullable String failReason
) {
}
