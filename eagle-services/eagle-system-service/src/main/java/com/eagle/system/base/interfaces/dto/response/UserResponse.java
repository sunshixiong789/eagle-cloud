package com.eagle.system.base.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * @author eagle
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

    @Schema(description = "认证账号ID", example = "10")
    private Long accountId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "真实姓名", example = "张三")
    private String name;

    @Schema(description = "昵称", example = "小张")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "当前是否在线")
    private boolean online;

    @Schema(description = "登录状态：ONLINE 在线，OFFLINE 离线")
    private String loginStatus;

    @Schema(description = "账号是否已加入黑名单")
    private boolean blacklisted;

    @Schema(description = "账号黑名单记录 ID")
    private Long blacklistId;

    @Schema(description = "已分配角色列表")
    private List<AssignedRoleResponse> roles;
}
