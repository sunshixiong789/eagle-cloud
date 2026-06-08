package com.eagle.payment.core.interfaces.dto.request;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
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
 * 创建支付订单请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "创建支付订单请求")
public class CreatePaymentRequest {

    @NotBlank
    @Size(max = 64)
    @Schema(description = "业务订单号 (上游调用方提供,与 channel 共同构成幂等键)",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "ORD20260608001")
    private String bizOrderNo;

    @NotNull
    @Schema(description = "支付渠道", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ALIPAY")
    private PaymentChannel channel;

    @NotNull
    @Schema(description = "支付场景", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PC_WEB")
    private PaymentScene scene;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 16, fraction = 2)
    @Schema(description = "支付金额(元)", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "99.00")
    private BigDecimal amount;

    @Schema(description = "币种,默认 CNY", example = "CNY")
    private String currency = "CNY";

    @NotBlank
    @Size(max = 256)
    @Schema(description = "订单标题", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Eagle 平台月度会员")
    private String subject;

    @Size(max = 64)
    @Schema(description = "下单用户 ID", example = "u100086")
    @Nullable
    private String userId;

    @Schema(description = "客户端 IP (微信 H5 必填)", example = "203.0.113.10")
    @Nullable
    private String clientIp;

    @Schema(description = "支付完成跳转 URL (支付宝 PC_WEB / MOBILE_WEB)",
            example = "https://app.example.com/order/paid")
    @Nullable
    private String returnUrl;

    @Schema(description = "微信 JSAPI / 小程序 OpenId (JSAPI/MINI_PROGRAM 场景必填)",
            example = "o9hWO5_xxx")
    @Nullable
    private String openId;

    @Schema(description = "过期分钟数 (默认 30)", example = "30")
    @Nullable
    private Integer expireMinutes;
}
