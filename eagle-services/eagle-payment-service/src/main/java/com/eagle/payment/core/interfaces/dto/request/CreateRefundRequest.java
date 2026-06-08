package com.eagle.payment.core.interfaces.dto.request;

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
 * 发起退款请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "发起退款请求")
public class CreateRefundRequest {

    @NotNull
    @Schema(description = "支付订单 ID", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1024")
    private Long paymentId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "业务退款号 (上游提供,幂等键)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "REF20260608001")
    private String bizRefundNo;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 16, fraction = 2)
    @Schema(description = "退款金额 (元)。部分退 = 小于原订单金额;全退 = 等于原订单可退余额",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "30.00")
    private BigDecimal amount;

    @Size(max = 512)
    @Schema(description = "退款原因", example = "用户申请部分退货")
    @Nullable
    private String reason;
}
