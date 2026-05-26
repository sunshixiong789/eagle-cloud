/**
 * 认证授权核心域（Auth Bounded Context）。
 *
 * <p><strong>职责</strong>：用户身份认证、OAuth2 授权服务器、微信/短信/号码一键登录、
 * 账号生命周期管理、JWT 签发与黑名单。
 *
 * <p><strong>内部分层</strong>（DDD，方向：interfaces → application → domain ← infrastructure）：
 * <ul>
 *   <li>{@code core.interfaces}     — REST Controller 与 DTO</li>
 *   <li>{@code core.application}    — 应用服务（用例编排、事务边界）</li>
 *   <li>{@code core.domain}         — 聚合根 / 值对象 / 领域事件 / Driven Port</li>
 *   <li>{@code core.infrastructure} — JPA、远程客户端、安全适配器、事件处理器</li>
 * </ul>
 *
 * <p><strong>对外公开的命名接口</strong>（其他服务/模块通过远程 API 访问，仅作历史标识）：
 * <ul>
 *   <li>{@code auth.core::domain-services} — WechatService、SmsService、PasswordEncryptor</li>
 *   <li>{@code auth.core::security}        — Filter、Provider、Converter</li>
 *   <li>{@code auth.core::port}            — Driven Port 接口</li>
 *   <li>{@code auth.core::event}           — 跨服务集成事件契约（仅 JSON 契约层面）</li>
 *   <li>{@code auth.core::repository}      — AccountRepository（曾供 system AdminInitializer）</li>
 *   <li>{@code auth.core::domain-model}    — Account 等核心模型（曾供 system Adapter 构造）</li>
 * </ul>
 *
 * <p><strong>跨服务集成</strong>（auth-service 已拆分为独立服务）：
 * <ul>
 *   <li>RocketMQ topic {@code eagle_auth_events} 发布 account.registered / account.deleted</li>
 *   <li>暴露 {@code /internal/online-users/**} 与 {@code /internal/account-blacklist/**} 内部端点</li>
 *   <li>调用 system-service 的 {@code /internal/authorization/{accountId}} 构造 JWT claims</li>
 * </ul>
 *
 * <p><strong>依赖约束</strong>：{@code allowedDependencies = {}} 表示本模块不依赖任何
 * 业务模块（仅隐式依赖 {@code common} 共享内核）。
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "认证授权核心域",
        allowedDependencies = {}
)
@NullMarked
package com.eagle.auth.core;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
