package com.eagle.system.auth.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自主注册请求
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterAccountRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 邮箱（可选，传给 system 创建 User）
     */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 昵称（可选）
     */
    @Schema(description = "昵称", example = "小张")
    private String nickname;
}
