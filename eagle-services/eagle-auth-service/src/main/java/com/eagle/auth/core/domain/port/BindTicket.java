package com.eagle.auth.core.domain.port;

import com.eagle.auth.core.domain.model.enums.SocialProvider;
import com.eagle.auth.core.domain.model.enums.WechatChannel;

/**
 * 第三方首登「待绑定」凭证。
 *
 * <p>第三方身份验签成功但未挂靠任何账号时，Provider 生成本凭证写入
 * {@link BindTicketStore}，以 {@code binding_required} 错误返回 ticketId；
 * 客户端补手机号验证码后走 {@code social_bind} grant 消费凭证完成挂靠。
 * 凭证内只存<b>验签后的结果</b>，social_bind 阶段不再回调第三方。
 *
 * @param provider                    第三方提供方
 * @param identifier                  第三方身份标识（openUid / apple subject / 微信本渠道 openid）
 * @param wechatChannel               微信渠道（provider=WECHAT 时必填，决定挂接的 openid 字段）
 * @param unionid                     微信 unionid（可空）
 * @param nickname                    昵称提示（可空，仅新建主账号时使用）
 * @param avatar                      头像提示（可空，仅新建主账号时使用）
 * @param appleEmail                  Apple 已验证邮箱（可空）
 * @param appleFullName               Apple 首次授权展示名（可空）
 * @param appleRefreshTokenCiphertext Apple refresh token 密文（provider=APPLE 时必填）
 * @author sunshixiong
 */
public record BindTicket(
        SocialProvider provider,
        String identifier,
        WechatChannel wechatChannel,
        String unionid,
        String nickname,
        String avatar,
        String appleEmail,
        String appleFullName,
        String appleRefreshTokenCiphertext
) {

    public static BindTicket ofTaobao(String openUid) {
        return new BindTicket(SocialProvider.TAOBAO, openUid,
                null, null, null, null, null, null, null);
    }

    public static BindTicket ofApple(String subject, String email, String fullName,
                                     String refreshTokenCiphertext) {
        return new BindTicket(SocialProvider.APPLE, subject,
                null, null, null, null, email, fullName, refreshTokenCiphertext);
    }

    public static BindTicket ofWechat(WechatChannel channel, String openid, String unionid,
                                      String nickname, String avatar) {
        return new BindTicket(SocialProvider.WECHAT, openid,
                channel, unionid, nickname, avatar, null, null, null);
    }
}
