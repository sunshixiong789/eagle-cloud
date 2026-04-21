/**
 * 系统管理模块（System Bounded Context）
 * <p>
 * 职责：用户、角色、权限、部门、菜单、岗位、字典、系统日志的管理。
 * <p>
 * <strong>与 auth 模块的关系（零依赖，事件驱动）</strong>
 * <ul>
 *   <li>system 域不依赖 auth 域的任何类型</li>
 *   <li>通过 common 包中的跨域事件契约（{@code AccountRegisteredEvent}、
 *       {@code AccountDeletedEvent}）接收 auth 域的通知</li>
 *   <li>system 基础设施层实现 auth 域的 {@code AuthorizationPort}（六边形架构 Driven Adapter）</li>
 * </ul>
 * <p>
 * <strong>依赖约束</strong>
 * <ul>
 *   <li>依赖 {@code auth::port}（实现 AuthorizationPort 接口）</li>
 *   <li>依赖 {@code common}（共享内核：跨域事件、异常体系）</li>
 *   <li>禁止依赖 {@code config}（避免循环）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "系统管理模块",
        allowedDependencies = {"auth::port", "common"}
)
@NullMarked
package com.eagle.system.system;

import org.jspecify.annotations.NullMarked;
