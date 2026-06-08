package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.RefundMapper;
import com.eagle.payment.core.application.service.RefundApplicationService;
import com.eagle.payment.core.interfaces.dto.request.CreateRefundRequest;
import com.eagle.payment.core.interfaces.dto.response.RefundResponse;
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
 * Refund 内部 REST 入口。
 *
 * @author sunshixiong
 */
@Tag(name = "退款 (内部)", description = "服务间调用,/internal/refunds")
@RestController
@RequestMapping("/internal/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundApplicationService refundApplicationService;
    private final RefundMapper mapper;

    @Operation(summary = "发起退款",
            description = "支持部分退 (取决于 eagle.payment.refund.allow-partial 开关);" +
                    "幂等键 (bizRefundNo)")
    @PreAuthorize("hasRole('service')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse create(@Valid @RequestBody CreateRefundRequest request) {
        return mapper.toResponse(refundApplicationService.create(request));
    }

    @Operation(summary = "查询退款单详情")
    @PreAuthorize("hasRole('service')")
    @GetMapping("/{id}")
    public RefundResponse get(@PathVariable Long id) {
        return mapper.toResponse(refundApplicationService.findById(id));
    }

    @Operation(summary = "按业务退款号查询")
    @PreAuthorize("hasRole('service')")
    @GetMapping
    public RefundResponse findByBizRefundNo(@RequestParam String bizRefundNo) {
        return mapper.toResponse(refundApplicationService.findByBizRefundNo(bizRefundNo));
    }
}
