package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.TransferMapper;
import com.eagle.payment.core.application.service.TransferApplicationService;
import com.eagle.payment.core.interfaces.dto.request.CreateTransferRequest;
import com.eagle.payment.core.interfaces.dto.response.TransferResponse;
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
 * Transfer 用户 REST 入口 (提现申请)。
 *
 * <p>权限模型: 登录用户均可访问;只能查询自己发起的提现单。审核走 {@link TransferAdminController}。
 *
 * @author sunshixiong
 */
@Tag(name = "提现", description = "用户视角的提现申请 / 查询")
@RestController
@RequestMapping("/internal/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferApplicationService transferApplicationService;
    private final TransferMapper mapper;

    @Operation(summary = "发起提现",
            description = "需在 eagle.payment.transfer.enabled=true 时启用;走单笔 / 日累计 / 日笔数风控")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request) {
        return mapper.toResponse(transferApplicationService.create(
                request, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询提现单详情 (仅本人)")
    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) {
        return mapper.toResponse(transferApplicationService.findById(
                id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "按业务提现号查询 (仅本人)")
    @GetMapping
    public TransferResponse findByBizTransferNo(@RequestParam String bizTransferNo) {
        return mapper.toResponse(transferApplicationService.findByBizTransferNo(
                bizTransferNo, SecurityUtils.getCurrentUserId()));
    }
}
