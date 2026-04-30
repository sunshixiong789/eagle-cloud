package com.eagle.system.base.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户请求
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Data
@Schema(description = "更新用户请求")
public class UpdateUserRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "真实姓名", example = "张三")
    private String name;

    @Schema(description = "昵称", example = "小张")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;
}
