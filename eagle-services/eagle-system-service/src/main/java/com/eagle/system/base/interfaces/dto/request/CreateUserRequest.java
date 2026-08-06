package com.eagle.system.base.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求
 *
 * @author eagle
 * @since 1.0.0
 */
@Schema(description = "创建用户请求")
public record CreateUserRequest(

        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 64, message = "用户名长度必须在2-64个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "用户名只能包含字母、数字、下划线和中划线")
        @Schema(description = "用户名", example = "zhangsan")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
        @Schema(description = "密码", example = "123456")
        String password,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        @Schema(description = "手机号", example = "13800138000")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Schema(description = "邮箱", example = "zhangsan@example.com")
        String email,

        @Schema(description = "真实姓名", example = "张三")
        String name,

        @Schema(description = "昵称", example = "小张")
        String nickname,

        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        String avatar,

        @Schema(description = "角色ID列表", example = "[1, 2]")
        Long[] roleIds
) {
}
