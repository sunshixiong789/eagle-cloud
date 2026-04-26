package com.eagle.system.common.event;

import java.util.Set;

/**
 * 账号注册事件（跨域事件契约）
 * <p>
 * auth 域创建 Account 后发布此事件，system 域订阅后创建对应的 User。
 * 事件类放在 common 包中作为微服务共享契约，避免模块间的直接依赖。
 * <p>
 * profileHints 字段为可选项，用于管理员创建用户时携带组织信息（部门、角色），
 * system 域的事件处理器可据此直接创建带部门和角色的 User。
 *
 * @author sunshixiong
 */
public record AccountRegisteredEvent(
        // 账号 ID
        Long accountId,

        // 用户名（system 域冗余存储，用于显示和搜索）
        String username,

        // 手机号（可选）
        String phone,

        // 昵称（微信登录时可能有值）
        String nickname,

        // 头像 URL（微信登录时可能有值）
        String avatar,

        // ==================== Profile Hints（可选，管理员创建时传入）====================

        // 邮箱（可选，传入时写入 User）
        String email,

        // 部门 ID（可选，传入时自动分配部门）
        Long deptId,

        // 角色 ID 集合（可选，传入时自动分配角色）
        Set<Long> roleIds
) {
}
