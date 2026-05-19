package com.eagle.system.auth.interfaces.dto.request;

import com.eagle.system.auth.domain.model.enums.BlacklistType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 添加黑名单请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "添加黑名单请求")
public class AddBlacklistRequest {

    @NotNull
    @Schema(description = "黑名单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PHONE")
    private BlacklistType type;

    @NotBlank
    @Size(max = 128)
    @Schema(description = "黑名单值", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String value;

    @Size(max = 255)
    @Schema(description = "加黑原因", example = "异常账号")
    private String reason;

    @Schema(description = "过期时间（null = 永久）", example = "2026-06-01T00:00:00")
    private LocalDateTime expiresAt;
}
