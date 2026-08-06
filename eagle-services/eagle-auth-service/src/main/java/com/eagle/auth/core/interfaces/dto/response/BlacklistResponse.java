package com.eagle.auth.core.interfaces.dto.response;

import com.eagle.auth.core.domain.model.enums.BlacklistType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 黑名单响应
 *
 * @author sunshixiong
 */
@Schema(description = "黑名单条目")
public record BlacklistResponse(

        @Schema(description = "黑名单ID", example = "1")
        Long id,

        @Schema(description = "黑名单类型", example = "PHONE")
        BlacklistType type,

        @Schema(description = "黑名单值", example = "13800138000")
        String value,

        @Schema(description = "加黑原因", example = "异常账号")
        String reason,

        @Schema(description = "过期时间", example = "2026-06-01T00:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "操作员ID", example = "1001")
        Long operatorId,

        @Schema(description = "操作员名称", example = "admin")
        String operatorName,

        @Schema(description = "创建时间", example = "2026-05-19T10:00:00")
        LocalDateTime createTime
) {
}
