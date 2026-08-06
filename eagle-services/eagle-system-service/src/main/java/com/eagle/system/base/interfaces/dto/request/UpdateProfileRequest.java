package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

/**
 * 更新当前用户档案请求（{@code PUT /users/me}）。
 *
 * <p>与 {@link UpdateUserRequest} 的区别：不含 {@code userId}。当前用户由 token 中的
 * accountId 解析，前端无需（也不应）传入 ID，避免越权与 accountId/userId 混淆。
 *
 * @author sunshixiong
 */
@Schema(description = "更新当前用户档案请求")
public record UpdateProfileRequest(

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
