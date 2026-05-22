/**
 * 认证授权模块（Auth Bounded Context）
 * <p>
 * 职责：用户身份认证、OAuth2 授权服务器、微信/短信第三方登录、账号管理。
 * <p>
 * <strong>对外公开的命名接口</strong>
 * <ul>
 *   <li>{@code auth::domain-services} — WechatService、SmsService</li>
 *   <li>{@code auth::security}        — Filter、Provider、Converter</li>
 * </ul>
 * <p>
 * <strong>依赖约束</strong>
 * <ul>
 *   <li>允许访问 {@code common} 共享内核（隐式，无需声明）</li>
 *   <li>禁止依赖 {@code system} 模块（六边形架构：auth 定义端口，system 实现端口）</li>
 *   <li>禁止依赖 {@code config} 模块</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "认证授权模块",
        // allowedDependencies = {} 即"不允许任何模块依赖"（common 因为 @Modulithic(sharedModules)
        // 隐式开放，无需写）。此声明把"禁止依赖 base/config"从注释升级为 ModulithArchitectureTest 硬约束。
        allowedDependencies = {}
)
@NullMarked
package com.eagle.auth;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
