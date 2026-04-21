package com.eagle.system.system.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

/**
 * 分配岗位请求
 *
 * @author sunshixiong
 * @since 1.0.0
 */
@Data
@Schema(description = "分配岗位请求")
public class AssignPostsRequest {

    @NotNull(message = "岗位ID集合不能为空")
    @Schema(description = "岗位ID集合", example = "[1, 2, 3]")
    private Set<Long> postIds;
}
