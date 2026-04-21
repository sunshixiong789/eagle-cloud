package com.eagle.system.auth.domain.service;

/**
 * 微信 Web 端 OAuth2 服务（领域服务接口）
 * <p>
 * 支持两种场景：
 * <ul>
 *   <li>PC 扫码登录：微信开放平台网站应用（scope: snsapi_login）</li>
 *   <li>H5 网页授权：微信公众号（scope: snsapi_userinfo）</li>
 * </ul>
 *
 * @author sunshixiong
 */
public interface WechatWebService {

    /**
     * PC 扫码登录：用微信开放平台 code 换取用户信息
     *
     * @param code 微信回调的临时授权 code
     * @return 微信用户信息
     */
    WechatWebUserInfo exchangePcCode(String code);

    /**
     * H5 网页授权：用公众号 code 换取用户信息
     *
     * @param code 微信回调的临时授权 code
     * @return 微信用户信息
     */
    WechatWebUserInfo exchangeH5Code(String code);

    /**
     * 微信 Web 用户信息
     *
     * @param openid   当前平台 openid（PC 为开放平台 openid，H5 为公众号 openid）
     * @param unionid  跨平台联合 ID（同一微信用户在同一开放平台账号下唯一，可能为 null）
     * @param nickname 微信昵称
     * @param avatar   微信头像 URL
     * @param channel  登录渠道：{@code "pc"} 或 {@code "h5"}
     */
    record WechatWebUserInfo(
            String openid,
            String unionid,
            String nickname,
            String avatar,
            String channel
    ) {
    }
}
