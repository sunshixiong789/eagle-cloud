package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.TransferMapper;
import com.eagle.payment.core.application.service.TransferApplicationService;
import com.eagle.payment.core.interfaces.dto.request.ApproveTransferRequest;
import com.eagle.payment.core.interfaces.dto.request.RejectTransferRequest;
import com.eagle.payment.core.interfaces.dto.request.TransferAdminQueryRequest;
import com.eagle.payment.core.interfaces.dto.response.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer 管理后台 REST 入口:审核 / 列表 / 详情。
 *
 * <p>权限: {@code payment:transfer:approve}。审核者身份从 JWT subject 取,前端不传。
 *
 * @author sunshixiong
 */
@Tag(name = "提现 (管理后台)", description = "审核 / 拒绝 / 待审列表")
@RestController
@RequestMapping("/admin/transfers")
@RequiredArgsConstructor
public class TransferAdminController {

    private final TransferApplicationService transferApplicationService;
    private final TransferMapper mapper;

    @Operation(summary = "审核提现列表",
            description = "支持按 mode / status / channel / bizTransferNo / 时间区间过滤")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @GetMapping
    public Page<TransferResponse> list(@ParameterObject TransferAdminQueryRequest query,
                                       @ParameterObject
                                       @Parameter(description = "分页参数")
                                       @PageableDefault Pageable pageable) {
        return transferApplicationService.queryForAdmin(query, pageable)
                .map(mapper::toResponse);
    }

    @Operation(summary = "查询提现单详情")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable Long id) {
        return mapper.toResponse(transferApplicationService.adminFindById(id));
    }

    @Operation(summary = "审核通过 (同事务调渠道)")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @PostMapping("/{id}/approve")
    public TransferResponse approve(@PathVariable Long id,
                                    @Valid @RequestBody ApproveTransferRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(transferApplicationService.approve(
                id, jwt.getSubject(), request.getRemark()));
    }

    @Operation(summary = "审核拒绝 (必填理由)")
    @PreAuthorize("hasAuthority('payment:transfer:approve')")
    @PostMapping("/{id}/reject")
    public TransferResponse reject(@PathVariable Long id,
                                   @Valid @RequestBody RejectTransferRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return mapper.toResponse(transferApplicationService.reject(
                id, jwt.getSubject(), request.getReason()));
    }
}
