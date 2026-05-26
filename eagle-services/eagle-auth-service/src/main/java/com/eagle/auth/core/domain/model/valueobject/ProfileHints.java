package com.eagle.auth.core.domain.model.valueobject;

/**
 * 用户画像提示（瞬态值对象）
 * <p>
 * 在 Account 工厂方法中设置，通过 {@code @PostPersist} 回调传递给
 * {@code AccountRegisteredEvent}，使 system 域能创建带正确 profile 的 User。
 * 事件发布后自动清除，不持久化到数据库。
 *
 * @param nickname 昵称（微信登录或管理员填写）
 * @param avatar   头像 URL（微信登录时携带）
 * @param email    邮箱（管理员创建时填写）
 * @author sunshixiong
 */
public record ProfileHints(
        String nickname,
        String avatar,
        String email
) {

    /**
     * 空画像提示（社交/短信登录自动注册时使用）
     */
    public static final ProfileHints EMPTY = new ProfileHints(null, null, null);

    /**
     * 微信登录专用（仅携带昵称和头像）
     */
    public static ProfileHints ofWechat(String nickname, String avatar) {
        return new ProfileHints(nickname, avatar, null);
    }
}
