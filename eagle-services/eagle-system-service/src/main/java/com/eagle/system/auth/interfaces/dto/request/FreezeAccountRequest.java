package com.eagle.system.auth.interfaces.dto.request;

import com.eagle.system.auth.domain.model.enums.FreezeReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冻结账号请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "冻结账号请求")
public class FreezeAccountRequest {

    @NotNull
    @Schema(description = "冻结原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "ADMIN")
    private FreezeReason reason;

    @Schema(description = "冻结到期时间（null = 永久）", example = "2026-06-01T00:00:00")
    private LocalDateTime freezeUntil;

    @Schema(description = "冻结备注", example = "违规操作")
    private String remark;
}
