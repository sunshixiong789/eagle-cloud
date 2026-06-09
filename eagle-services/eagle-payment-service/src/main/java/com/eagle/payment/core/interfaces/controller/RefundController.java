package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.RefundMapper;
import com.eagle.payment.core.application.service.RefundApplicationService;
import com.eagle.payment.core.interfaces.dto.request.CreateRefundRequest;
import com.eagle.payment.core.interfaces.dto.response.RefundResponse;
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
 * Refund 用户 REST 入口。
 *
 * <p>权限模型: 登录用户均可访问;只能对自己已支付订单发起退款 / 查询自己的退款单。
 *
 * @author sunshixiong
 */
@Tag(name = "退款", description = "用户视角的退款发起 / 查询")
@RestController
@RequestMapping("/internal/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundApplicationService refundApplicationService;
    private final RefundMapper mapper;

    @Operation(summary = "发起退款 (仅退本人订单)",
            description = "支持部分退 (取决于 eagle.payment.refund.allow-partial 开关);" +
                    "幂等键 (bizRefundNo)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse create(@Valid @RequestBody CreateRefundRequest request) {
        return mapper.toResponse(refundApplicationService.create(
                request, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询退款单详情 (仅本人)")
    @GetMapping("/{id}")
    public RefundResponse get(@PathVariable Long id) {
        return mapper.toResponse(refundApplicationService.findById(
                id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "按业务退款号查询 (仅本人)")
    @GetMapping
    public RefundResponse findByBizRefundNo(@RequestParam String bizRefundNo) {
        return mapper.toResponse(refundApplicationService.findByBizRefundNo(
                bizRefundNo, SecurityUtils.getCurrentUserId()));
    }
}
