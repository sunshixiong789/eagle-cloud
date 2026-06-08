package com.eagle.payment.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * 取消支付订单请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "取消支付订单请求")
public class CancelPaymentRequest {

    @Size(max = 512)
    @Schema(description = "取消原因", example = "用户主动取消订单")
    @Nullable
    private String reason;
}
