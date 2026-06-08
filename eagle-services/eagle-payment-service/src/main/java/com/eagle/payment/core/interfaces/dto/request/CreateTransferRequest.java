package com.eagle.payment.core.interfaces.dto.request;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 发起提现请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "发起提现请求")
public class CreateTransferRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "业务提现号 (上游提供,幂等键)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "TRN20260608001")
    private String bizTransferNo;

    @NotNull
    @Schema(description = "提现渠道", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ALIPAY")
    private PaymentChannel channel;

    @NotNull
    @Schema(description = "受理模式:IMMEDIATE 立即到账 / APPROVAL 需审核",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVAL")
    private TransferMode mode;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "收款方账号 (支付宝登录号 / 微信 openId)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    private String recipientAccount;

    @Size(max = 128)
    @Schema(description = "收款方姓名 (实名校验,部分渠道必填)", example = "张三")
    @Nullable
    private String recipientName;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 16, fraction = 2)
    @Schema(description = "提现金额(元)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    private BigDecimal amount;

    @Size(max = 512)
    @Schema(description = "提现说明", example = "卖家月度结算")
    @Nullable
    private String reason;
}
