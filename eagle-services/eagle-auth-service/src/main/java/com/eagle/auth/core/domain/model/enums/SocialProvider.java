package com.eagle.auth.core.domain.model.enums;

/**
 * 第三方登录提供方。
 *
 * <p>手机号为主账号体系下，第三方身份只是登录方式，
 * 首次登录必须通过 social_bind 挂靠到手机号主账号。
 *
 * @author sunshixiong
 */
public enum SocialProvider {

    /** 淘宝（openUid） */
    TAOBAO,

    /** Apple Sign In（subject） */
    APPLE,

    /** 微信（多渠道 openid + unionid，见 {@link WechatChannel}） */
    WECHAT
}
