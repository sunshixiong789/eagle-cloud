package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手机号一键登录配置属性
 * <p>
 * 对应 application.yml 中的 {@code eagle.auth.one-click} 前缀配置。
 *
 * <p><strong>当前状态：功能未上线，默认关闭。</strong>唯一在册的
 * {@link com.eagle.auth.core.infrastructure.external.provider.PhoneOneClickProvider} 实现是
 * {@code MockPhoneOneClickProvider}，它把 access_token 直接当手机号用 —— 一旦在生产可达，
 * 等同于「知道手机号即可登录该账号」。因此这里用三道各自独立的锁把它封住：
 * <ol>
 *   <li>{@code enabled} 默认 {@code false}（本类）；</li>
 *   <li>mock provider 标了 {@code @Profile("dev")}，非 dev 环境根本不注册；</li>
 *   <li>{@code phone_one_click} 已从 application.yml 与
 *       {@link OAuthClientProperties} / {@link OAuthAppClientProperties} 的默认授权类型中移除，
 *       客户端拿不到这个 grant。</li>
 * </ol>
 *
 * <p>接入真实运营商 / 聚合网关时：新增一个实现 {@code PhoneOneClickProvider} 的类
 * （原阿里云 dypnsapi、腾讯云 PNSV 两个实现已随其 SDK 依赖一并移除，需要时从 git 历史取回），
 * 配上凭证，把上面三道锁逐一打开，并补一条真实 provider 的集成验证。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.auth.one-click")
public class PhoneOneClickProperties {

    /**
     * 是否启用一键登录。默认 {@code false} —— 在接入真实 provider 之前不要打开，
     * 详见类注释。
     */
    private boolean enabled = false;

    /**
     * 提供方标识，与 {@code PhoneOneClickProvider#name()} 对齐（不区分大小写）。
     * 目前仅 {@code mock}（限 dev profile）。
     */
    private String provider = "mock";
}
