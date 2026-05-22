package com.eagle.auth.interfaces.controller;

import com.eagle.common.dto.EagleUser;
import com.eagle.auth.application.command.AddBlacklistCommand;
import com.eagle.auth.application.command.BlacklistQuery;
import com.eagle.auth.application.service.BlacklistApplicationService;
import com.eagle.auth.domain.model.enums.BlacklistType;
import com.eagle.auth.interfaces.dto.request.AddBlacklistRequest;
import com.eagle.auth.interfaces.dto.response.BlacklistResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份黑名单管理控制器（租户级，仅管理员）
 *
 * @author sunshixiong
 */
@Tag(name = "黑名单管理", description = "租户级身份黑名单的增删查")
@RestController
@RequestMapping("/admin/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistApplicationService blacklistApplicationService;

    @Operation(summary = "查询黑名单（分页）")
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    public Page<BlacklistResponse> query(
            @RequestParam(required = false) BlacklistType type,
            @RequestParam(required = false) String value,
            @ParameterObject
            @Parameter(description = "分页参数（page 从 0 开始）")
            @PageableDefault Pageable pageable) {
        return blacklistApplicationService.queryBlacklist(new BlacklistQuery(type, value), pageable);
    }

    @Operation(summary = "新增黑名单条目")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public BlacklistResponse add(@Valid @RequestBody AddBlacklistRequest request,
                                 @AuthenticationPrincipal EagleUser principal) {
        return blacklistApplicationService.addToBlacklist(
                new AddBlacklistCommand(
                        request.getType(), request.getValue(), request.getReason(),
                        request.getExpiresAt(),
                        principal != null ? principal.getId() : null,
                        principal != null ? principal.getName() : "admin"));
    }

    @Operation(summary = "删除黑名单条目")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void remove(@Parameter(description = "黑名单ID") @PathVariable Long id) {
        blacklistApplicationService.removeFromBlacklist(id);
    }
}
