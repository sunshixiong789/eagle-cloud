package com.eagle.auth.core.domain.model.enums;

/**
 * 微信登录渠道。
 *
 * <p>同一微信主体在不同渠道的 openid 不同（unionid 相同），
 * 各渠道 openid 落在 {@code WechatBinding} 的不同字段。
 *
 * @author sunshixiong
 */
public enum WechatChannel {

    /** 小程序（{@code openid} 字段） */
    MINI_PROGRAM,

    /** 开放平台移动应用（{@code web_openid} 字段，与 PC 同属开放平台） */
    APP,

    /** 开放平台网站应用 PC 扫码（{@code web_openid} 字段） */
    PC,

    /** 公众号网页授权 H5（{@code mp_openid} 字段） */
    H5;

    /** 该渠道 openid 是否落在开放平台 {@code web_openid} 字段。 */
    public boolean isOpenPlatform() {
        return this == APP || this == PC;
    }
}
