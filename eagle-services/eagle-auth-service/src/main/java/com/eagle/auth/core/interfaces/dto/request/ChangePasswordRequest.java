package com.eagle.auth.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求
 *
 * @author sunshixiong
 */
@Schema(description = "修改密码请求")
public record ChangePasswordRequest(

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
        @Schema(description = "新密码", example = "newpass123", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {
}
