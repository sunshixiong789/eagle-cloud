package com.eagle.system.base.web.dto.request;

import com.eagle.system.base.domain.model.enums.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色条件查询请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "角色条件查询请求")
public class RoleQueryRequest {

    /** 角色名称（模糊匹配） */
    @Schema(description = "角色名称（模糊匹配）")
    private String roleName;

    /** 角色标识（精确匹配） */
    @Schema(description = "角色标识（精确匹配）")
    private String roleCode;

    /** 状态 */
    @Schema(description = "状态")
    private RoleStatus status;
}
