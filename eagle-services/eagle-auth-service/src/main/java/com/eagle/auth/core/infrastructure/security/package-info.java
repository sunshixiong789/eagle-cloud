/**
 * Auth 模块安全基础设施组件（Named Interface "security"）
 * <p>
 * 包含 Spring Security 集成的核心认证组件，需对 {@code config} 模块的
 * SecurityConfig 开放，用于装配 OAuth2 授权服务器和安全过滤链：
 * <ul>
 *   <li>{@code LoginRateLimitFilter}                    — 登录频率限制过滤器</li>
 *   <li>{@code SmsCodeAuthenticationConverter}          — 短信验证码请求转换器</li>
 *   <li>{@code SmsCodeAuthenticationProvider}           — 短信验证码认证提供者</li>
 *   <li>{@code WechatMiniProgramAuthenticationConverter} — 微信小程序请求转换器</li>
 *   <li>{@code WechatMiniProgramAuthenticationProvider} — 微信小程序认证提供者</li>
 * </ul>
 * <p>
 * <strong>架构说明</strong>
 * <p>
 * 安全组件属于 auth 的内部实现细节。理想情况下，SecurityConfig 应移入 auth 模块，
 * 但考虑到 SecurityConfig 还负责其他跨域基础设施（Cache、JPA、WebSocket 等的配合），
 * 通过此 Named Interface 精确声明需要对 config 开放的 auth 安全组件，
 * 作为当前过渡阶段的务实决策。
 *
 * @author sunshixiong
 */
@NamedInterface("security")
package com.eagle.auth.core.infrastructure.security;

import org.springframework.modulith.NamedInterface;