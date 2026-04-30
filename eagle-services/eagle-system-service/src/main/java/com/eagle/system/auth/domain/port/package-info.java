/**
 * Auth 模块的驱动端口（Driven Ports，六边形架构）
 * <p>
 * auth 领域层定义的出站端口接口，由外部适配器实现：
 * <ul>
 *   <li>{@link com.eagle.auth.domain.port.AuthorizationPort} — 授权信息查询端口</li>
 *   <li>{@link com.eagle.auth.domain.port.AuthorizationInfo} — 授权信息 DTO</li>
 *   <li>{@link com.eagle.auth.domain.port.OnlineUserPort} — 在线用户管理端口（Redis 追踪 + JWT 黑名单）</li>
 *   <li>{@link com.eagle.auth.domain.port.OnlineUserInfo} — 在线用户信息 DTO</li>
 * </ul>
 * <p>
 * 单体架构：system 基础设施层实现（{@code AuthorizationAdapter}）；
 * auth 基础设施层实现（{@code OnlineUserAdapter}）。<br>
 * 微服务拆分：auth 基础设施层换为远程实现（HTTP/gRPC），system 暴露 REST API。
 *
 * @author sunshixiong
 */
@NamedInterface("port")
package com.eagle.system.auth.domain.port;

import org.springframework.modulith.NamedInterface;