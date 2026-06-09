package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.PaymentMapper;
import com.eagle.payment.core.application.service.PaymentApplicationService;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.interfaces.dto.request.CancelPaymentRequest;
import com.eagle.payment.core.interfaces.dto.request.CreatePaymentRequest;
import com.eagle.payment.core.interfaces.dto.response.CreatePaymentResponse;
import com.eagle.payment.core.interfaces.dto.response.PaymentResponse;
import com.eagle.resource.server.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment 用户 REST 入口。
 *
 * <p>权限模型: 登录用户均可访问 (filter chain 已强制 authenticated);
 * 数据归属由 ApplicationService 内部按 JWT userId 校验,他人订单一律按 NOT_FOUND 返回。
 *
 * @author sunshixiong
 */
@Tag(name = "支付订单", description = "用户视角的支付订单创建 / 查询 / 取消")
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;
    private final PaymentMapper mapper;

    @Operation(summary = "创建支付订单",
            description = "幂等键: (bizOrderNo, channel);返回渠道支付参数 payload + payloadType。" +
                    "userId 自动取自 JWT,不接受入参传入")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentApplicationService.create(request, SecurityUtils.getCurrentUserId());
    }

    @Operation(summary = "查询支付订单详情 (仅本人)")
    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return mapper.toResponse(paymentApplicationService.findById(id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "按业务订单号 + 渠道查询 (仅本人)")
    @GetMapping
    public PaymentResponse findByBizOrderNo(
            @RequestParam String bizOrderNo,
            @RequestParam PaymentChannel channel) {
        return mapper.toResponse(paymentApplicationService.findByBizOrderNo(
                bizOrderNo, channel, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "主动取消支付订单 (仅本人)",
            description = "仅 CREATED / PAYING 状态允许;终态返回 INVALID_STATUS")
    @PostMapping("/{id}/cancel")
    public PaymentResponse cancel(@PathVariable Long id,
                                  @Valid @RequestBody CancelPaymentRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        paymentApplicationService.cancel(id, request.getReason(), currentUserId);
        return mapper.toResponse(paymentApplicationService.findById(id, currentUserId));
    }
}
