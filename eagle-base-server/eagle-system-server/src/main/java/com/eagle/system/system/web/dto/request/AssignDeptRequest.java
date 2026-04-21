package com.eagle.system.system.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分配部门请求
 *
 * @author sunshixiong
 * @since 1.0.0
 */
@Data
@Schema(description = "分配部门请求")
public class AssignDeptRequest {

    @NotNull(message = "部门ID不能为空")
    @Schema(description = "部门ID", example = "1")
    private Long deptId;
}
