package com.eagle.payment.core.interfaces.dto.request;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Admin 提现单列表查询条件。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "提现单列表查询条件")
public class TransferAdminQueryRequest {

    @Schema(description = "受理模式")
    @Nullable
    private TransferMode mode;

    @Schema(description = "状态")
    @Nullable
    private TransferStatus status;

    @Schema(description = "渠道")
    @Nullable
    private PaymentChannel channel;

    @Schema(description = "业务提现号 (精确匹配)")
    @Nullable
    private String bizTransferNo;

    @Schema(description = "创建时间区间起")
    @Nullable
    private LocalDateTime createTimeFrom;

    @Schema(description = "创建时间区间止")
    @Nullable
    private LocalDateTime createTimeTo;
}
