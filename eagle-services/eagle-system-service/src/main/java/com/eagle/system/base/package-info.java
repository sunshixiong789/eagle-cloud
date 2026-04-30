/**
 * 系统管理模块（System Bounded Context）
 * <p>
 * 职责：用户、角色、权限、部门、菜单、岗位、字典、系统日志的管理。
 * <p>
 * <strong>与 auth 模块的关系（事件驱动 + Port 适配）</strong>
 * <ul>
 *   <li>通过 {@code auth::event} 订阅跨域事件（{@code AccountRegisteredEvent}、
 *       {@code AccountDeletedEvent}），异步创建/删除对应的 User</li>
 *   <li>通过 {@code auth::port} 实现 {@code AuthorizationPort}（六边形架构 Driven Adapter）</li>
 * </ul>
 * <p>
 * <strong>依赖约束</strong>
 * <ul>
 *   <li>依赖 {@code auth::port}（实现 AuthorizationPort 接口）</li>
 *   <li>依赖 {@code auth::event}（订阅跨域事件）</li>
 *   <li>依赖 {@code common}（共享内核：异常体系、基础 DTO）</li>
 *   <li>禁止依赖 {@code config}（避免循环）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "系统管理模块",
        allowedDependencies = {"auth::port", "auth::event"}
)
@NullMarked
package com.eagle.system.base;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
