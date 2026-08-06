package com.eagle.auth.core.interfaces.dto.request;

import com.eagle.auth.core.domain.model.enums.BlacklistType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 添加黑名单请求
 *
 * @author sunshixiong
 */
@Schema(description = "添加黑名单请求")
public record AddBlacklistRequest(

        @NotNull
        @Schema(description = "黑名单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PHONE")
        BlacklistType type,

        @NotBlank
        @Size(max = 128)
        @Schema(description = "黑名单值", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
        String value,

        @Size(max = 255)
        @Schema(description = "加黑原因", example = "异常账号")
        String reason,

        @Schema(description = "过期时间（null = 永久）", example = "2026-06-01T00:00:00")
        LocalDateTime expiresAt
) {
}
