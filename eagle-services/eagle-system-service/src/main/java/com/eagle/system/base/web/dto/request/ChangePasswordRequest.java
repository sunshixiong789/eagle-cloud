package com.eagle.system.base.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码", example = "123456")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
    @Schema(description = "新密码", example = "654321")
    private String newPassword;
}
