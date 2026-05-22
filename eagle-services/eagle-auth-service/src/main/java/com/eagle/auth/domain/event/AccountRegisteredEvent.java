package com.eagle.auth.domain.event;

/**
 * 账号注册事件（跨域集成事件）
 * <p>
 * auth 域创建 Account 后发布此事件，system 域订阅后创建对应的 User。
 * 事件类放在 auth 域的 domain/event 包中，通过 Named Interface "event" 暴露给其他模块。
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

        // 邮箱（可选，传入时写入 User）
        String email
) {
}
