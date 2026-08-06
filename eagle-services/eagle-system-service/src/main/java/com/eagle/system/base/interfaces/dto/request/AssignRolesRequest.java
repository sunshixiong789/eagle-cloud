package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * 分配角色请求
 *
 * @author sunshixiong
 * @since 1.0.0
 */
@Schema(description = "分配角色请求")
public record AssignRolesRequest(

        @NotNull(message = "角色ID集合不能为空")
        @Schema(description = "角色ID集合", example = "[1, 2, 3]")
        Set<Long> roleIds
) {
}
