/**
 * 系统管理模块(System Bounded Context)
 * <p>
 * 职责:用户、角色、权限、部门、菜单、岗位、字典、系统日志的管理。
 * <p>
 * <strong>与 auth-service 的关系(跨服务集成,已拆分为独立服务)</strong>
 * <ul>
 *   <li>通过 RocketMQ topic {@code eagle_auth_events} 异步消费 auth-service
 *       的集成事件({@code account.registered} / {@code account.deleted}),
 *       在 base 域创建或删除对应 User。
 *   <li>通过 RestClient 同步调用 auth-service 的 {@code /internal/online-users/**}
 *       与 {@code /internal/account-blacklist/**} 内部端点。
 *   <li>反向暴露 {@code /internal/authorization/{accountId}} 供 auth-service
 *       构建 JWT claims(姓名 + 角色码)。
 * </ul>
 * <p>
 * <strong>依赖约束</strong>
 * <ul>
 *   <li>依赖 {@code common}(共享内核:异常体系、基础 DTO)</li>
 *   <li>禁止依赖 {@code config}(避免循环)</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "系统管理模块"
)
@NullMarked
package com.eagle.system.base;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
