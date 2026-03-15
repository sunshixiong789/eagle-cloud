package com.eagle.system.base.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户响应")
public class UserResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "真实姓名", example = "张三")
    private String name;

    @Schema(description = "昵称", example = "小张")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "是否锁定", example = "false")
    private Boolean locked;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;
}
