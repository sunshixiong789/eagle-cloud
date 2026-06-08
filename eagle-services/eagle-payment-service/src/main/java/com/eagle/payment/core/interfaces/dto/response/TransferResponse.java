package com.eagle.payment.core.interfaces.dto.response;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现单详情响应。
 *
 * @author sunshixiong
 */
@Schema(description = "提现单详情")
public record TransferResponse(
        @Schema(description = "提现单 ID") Long id,
        @Schema(description = "业务提现号") String bizTransferNo,
        @Schema(description = "渠道") PaymentChannel channel,
        @Schema(description = "收款方账号") String recipientAccount,
        @Schema(description = "收款方姓名") String recipientName,
        @Schema(description = "提现金额(元)") BigDecimal amount,
        @Schema(description = "提现说明") String reason,
        @Schema(description = "渠道转账单号") String channelTransferNo,
        @Schema(description = "状态") TransferStatus status,
        @Schema(description = "到账时间") LocalDateTime succeededAt,
        @Schema(description = "失败 / 退票原因") String failReason,
        @Schema(description = "创建时间") LocalDateTime createTime
) {
}
