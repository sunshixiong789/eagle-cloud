/**
 * Eagle Auth Service 应用根包。
 *
 * <p>本包仅承载 Spring Boot 应用入口 {@code EagleAuthApplication} 与
 * {@code @Modulithic} 声明，不包含业务代码。所有业务代码位于
 * {@link com.eagle.auth.core} 子包（单一有界上下文：认证授权核心域）。
 *
 * <p>选择"应用基础包 ≠ 模块基础包"的拓扑，是为了让
 * {@code ApplicationModules.of(EagleAuthApplication.class).verify()} 能将
 * {@code com.eagle.auth.core} 识别为单一模块（其下 {@code interfaces /
 * application / domain / infrastructure} 视为内部包），而不是把 DDD 四层
 * 误判为四个独立模块导致循环依赖告警。
 *
 * @author sunshixiong
 */
@NullMarked
package com.eagle.auth;

import org.jspecify.annotations.NullMarked;
