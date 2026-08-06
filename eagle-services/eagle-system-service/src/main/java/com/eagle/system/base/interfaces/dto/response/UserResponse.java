package com.eagle.system.base.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应对象
 *
 * @author eagle
 * @since 1.0.0
 */
@Schema(description = "用户响应")
public record UserResponse(

        @Schema(description = "用户ID", example = "1")
        Long id,

        @Schema(description = "认证账号ID", example = "10")
        Long accountId,

        @Schema(description = "用户名", example = "zhangsan")
        String username,

        @Schema(description = "手机号", example = "13800138000")
        String phone,

        @Schema(description = "邮箱", example = "zhangsan@example.com")
        String email,

        @Schema(description = "真实姓名", example = "张三")
        String name,

        @Schema(description = "昵称", example = "小张")
        String nickname,

        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        String avatar,

        @Schema(description = "创建时间")
        LocalDateTime createdAt,

        @Schema(description = "最后登录时间")
        LocalDateTime lastLoginAt,

        @Schema(description = "当前是否在线")
        boolean online,

        @Schema(description = "登录状态：ONLINE 在线，OFFLINE 离线")
        String loginStatus,

        @Schema(description = "账号是否已加入黑名单")
        boolean blacklisted,

        @Schema(description = "账号黑名单记录 ID")
        Long blacklistId,

        @Schema(description = "已分配角色列表")
        List<AssignedRoleResponse> roles
) {

    /**
     * 补齐跨聚合的富化字段：角色、最后登录时间、在线态、黑名单状态。
     * <p>
     * 这些数据来自 auth-service 与日志表，Mapper 拿不到，只能由应用服务查完后回填。
     * record 不可变，因此返回新实例而不是就地 set（见 rules/08-quality.md 第三节）。
     */
    public UserResponse withEnrichment(List<AssignedRoleResponse> assignedRoles,
                                       LocalDateTime lastLogin,
                                       boolean isOnline,
                                       boolean isBlacklisted,
                                       Long blacklistRecordId) {
        return new UserResponse(id, accountId, username, phone, email, name, nickname, avatar,
                createdAt, lastLogin, isOnline, isOnline ? "ONLINE" : "OFFLINE",
                isBlacklisted, blacklistRecordId, assignedRoles);
    }

    /** 补齐 auth-service 的权威手机号（system 域不持久化副本）。 */
    public UserResponse withPhone(String accountPhone) {
        return new UserResponse(id, accountId, username, accountPhone, email, name, nickname, avatar,
                createdAt, lastLoginAt, online, loginStatus, blacklisted, blacklistId, roles);
    }
}
