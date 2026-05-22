/**
 * Auth 模块的驱动端口（Driven Ports，六边形架构）
 * 单体架构：system 基础设施层实现（{@code AuthorizationAdapter}）；
 * auth 基础设施层实现（{@code OnlineUserAdapter}）。<br>
 * 微服务拆分：auth 基础设施层换为远程实现（HTTP/gRPC），system 暴露 REST API。
 *
 * @author sunshixiong
 */
@NamedInterface("port")
package com.eagle.auth.domain.port;

import org.springframework.modulith.NamedInterface;