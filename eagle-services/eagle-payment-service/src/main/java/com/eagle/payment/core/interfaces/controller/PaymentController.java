package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.PaymentMapper;
import com.eagle.payment.core.application.service.PaymentApplicationService;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.interfaces.dto.request.CancelPaymentRequest;
import com.eagle.payment.core.interfaces.dto.request.CreatePaymentRequest;
import com.eagle.payment.core.interfaces.dto.response.CreatePaymentResponse;
import com.eagle.payment.core.interfaces.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment 内部 REST 入口。
 *
 * @author sunshixiong
 */
@Tag(name = "支付订单 (内部)", description = "服务间调用,/internal/payments")
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;
    private final PaymentMapper mapper;

    @Operation(summary = "创建支付订单",
            description = "幂等键: (bizOrderNo, channel);返回渠道支付参数 payload + payloadType")
    @PreAuthorize("hasRole('service')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentApplicationService.create(request);
    }

    @Operation(summary = "查询支付订单详情")
    @PreAuthorize("hasRole('service')")
    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return mapper.toResponse(paymentApplicationService.findById(id));
    }

    @Operation(summary = "按业务订单号 + 渠道查询")
    @PreAuthorize("hasRole('service')")
    @GetMapping
    public PaymentResponse findByBizOrderNo(
            @RequestParam String bizOrderNo,
            @RequestParam PaymentChannel channel) {
        return mapper.toResponse(paymentApplicationService.findByBizOrderNo(bizOrderNo, channel));
    }

    @Operation(summary = "主动取消支付订单",
            description = "仅 CREATED / PAYING 状态允许;终态返回 INVALID_STATUS")
    @PreAuthorize("hasRole('service')")
    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable Long id,
                                  @Valid @RequestBody CancelPaymentRequest request) {
        paymentApplicationService.cancel(id, request.getReason());
        return mapper.toResponse(paymentApplicationService.findById(id));
    }
}
