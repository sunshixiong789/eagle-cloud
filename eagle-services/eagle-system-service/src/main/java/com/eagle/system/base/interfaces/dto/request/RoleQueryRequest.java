package com.eagle.system.base.interfaces.dto.request;

import com.eagle.system.base.domain.model.enums.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 角色条件查询请求
 *
 * @param roleName 角色名称（模糊匹配）
 * @param roleCode 角色标识（精确匹配）
 * @param status   状态
 * @author sunshixiong
 */
@Schema(description = "角色条件查询请求")
public record RoleQueryRequest(

        @Schema(description = "角色名称（模糊匹配）")
        String roleName,

        @Schema(description = "角色标识（精确匹配）")
        String roleCode,

        @Schema(description = "状态")
        RoleStatus status
) {
}
