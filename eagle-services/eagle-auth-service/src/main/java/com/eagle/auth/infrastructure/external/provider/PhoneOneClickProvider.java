package com.eagle.auth.infrastructure.external.provider;

/**
 * 手机号一键登录 Provider SPI
 * <p>
 * 每个 Provider 对应一种运营商网关 / 聚合 SDK（mock / 阿里云 / 腾讯云 / 极光 / 网易盾 …），由
 * {@code PhoneOneClickServiceImpl} 按 {@code eagle.auth.one-click.provider} 配置路由。
 * Provider 内部负责凭证初始化、SDK 调用、错误码映射、手机号格式校验，
 * 对上层只暴露"凭 access_token 换真实手机号"这一单一职责。
 *
 * @author sunshixiong
 */
public interface PhoneOneClickProvider {

    /**
     * Provider 唯一标识，与 {@code eagle.auth.one-click.provider} 配置值对齐（不区分大小写）
     */
    String name();

    /**
     * 校验运营商颁发的一键登录 access_token，并换取真实手机号
     *
     * @param accessToken 运营商 / SDK 颁发的短期访问凭证（已由上层做非空校验）
     * @return 真实手机号（11 位）
     */
    String verifyAndGetPhone(String accessToken);
}
