package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * 更新用户请求
 *
 * @author eagle
 * @since 1.0.0
 */
@Schema(description = "更新用户请求")
public record UpdateUserRequest(

        @NotNull(message = "用户ID不能为空")
        @Schema(description = "用户ID", example = "1")
        Long userId,

        @Schema(description = "真实姓名", example = "张三")
        String name,

        @Schema(description = "昵称", example = "小张")
        String nickname,

        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        String avatar,

        @Email(message = "邮箱格式不正确")
        @Schema(description = "邮箱", example = "zhangsan@example.com")
        String email
) {
}
