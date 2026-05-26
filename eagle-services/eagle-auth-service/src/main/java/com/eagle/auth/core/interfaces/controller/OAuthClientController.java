package com.eagle.auth.core.interfaces.controller;

import com.eagle.auth.core.application.service.OAuthClientApplicationService;
import com.eagle.auth.core.interfaces.dto.request.CreateOAuthClientRequest;
import com.eagle.auth.core.interfaces.dto.request.UpdateOAuthClientRequest;
import com.eagle.auth.core.interfaces.dto.response.OAuthClientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 客户端管理控制器
 *
 * @author sunshixiong
 */
@Tag(name = "OAuth客户端管理", description = "OAuth2 客户端的增删改查操作")
@RestController
@RequestMapping("oauth-clients")
@RequiredArgsConstructor
public class OAuthClientController {

    private final OAuthClientApplicationService oAuthClientApplicationService;

    @Operation(summary = "创建OAuth客户端", description = "创建新的 OAuth2 客户端")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public OAuthClientResponse createClient(@Valid @RequestBody CreateOAuthClientRequest request) {
        return oAuthClientApplicationService.createClient(request);
    }

    @Operation(summary = "更新OAuth客户端", description = "更新指定 OAuth2 客户端配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public OAuthClientResponse updateClient(@Parameter(description = "客户端ID") @PathVariable Long id,
                                            @Valid @RequestBody UpdateOAuthClientRequest request) {
        return oAuthClientApplicationService.updateClient(id, request);
    }

    @Operation(summary = "删除OAuth客户端", description = "删除指定 OAuth2 客户端")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteClient(@Parameter(description = "客户端ID") @PathVariable Long id) {
        oAuthClientApplicationService.deleteClient(id);
    }

    @Operation(summary = "查询OAuth客户端详情", description = "根据 ID 获取 OAuth2 客户端详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public OAuthClientResponse getClientById(@Parameter(description = "客户端ID") @PathVariable Long id) {
        return oAuthClientApplicationService.getClientById(id);
    }

    @Operation(summary = "查询OAuth客户端列表", description = "分页查询所有 OAuth2 客户端")
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Page<OAuthClientResponse> queryClients(@ParameterObject
                                                  @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                                  @PageableDefault Pageable pageable) {
        return oAuthClientApplicationService.queryClients(pageable);
    }

    @Operation(summary = "启用OAuth客户端", description = "启用指定的 OAuth2 客户端")
    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void enableClient(@Parameter(description = "客户端ID") @PathVariable Long id) {
        oAuthClientApplicationService.enableClient(id);
    }

    @Operation(summary = "禁用OAuth客户端", description = "禁用指定的 OAuth2 客户端")
    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void disableClient(@Parameter(description = "客户端ID") @PathVariable Long id) {
        oAuthClientApplicationService.disableClient(id);
    }
}
