package com.eagle.auth.core.interfaces.dto.request;

import com.eagle.auth.core.domain.model.enums.FreezeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 冻结账号请求
 *
 * @author sunshixiong
 */
@Schema(description = "冻结账号请求")
public record FreezeAccountRequest(

        @NotNull
        @Schema(description = "冻结原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "ADMIN")
        FreezeReason reason,

        @Schema(description = "冻结到期时间（null = 永久）", example = "2026-06-01T00:00:00")
        LocalDateTime freezeUntil,

        @Schema(description = "冻结备注", example = "违规操作")
        String remark
) {
}
