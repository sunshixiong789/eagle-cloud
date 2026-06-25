package com.eagle.auth.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改手机号请求（已登录用户自助改号）。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "修改手机号请求")
public class ChangePhoneRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "新手机号", example = "13900139000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 6, message = "验证码长度必须在4-6个字符之间")
    @Schema(description = "新手机号收到的短信验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}
