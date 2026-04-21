/**
 * 全局配置模块（Infrastructure Glue Code）
 * <p>
 * 非业务域模块，负责跨域基础设施的 Spring Bean 装配：
 * <ul>
 *   <li>SecurityConfig    — OAuth2 授权服务器、JWT、CORS、登录过滤链</li>
 *   <li>CacheConfig       — Redis + Caffeine 多级缓存</li>
 *   <li>AsyncConfig       — 异步线程池（@EnableAsync）</li>
 *   <li>I18nConfig        — 国际化（Locale 解析、拦截器）</li>
 *   <li>JpaConfig         — JPA 审计（AuditorAware）</li>
 *   <li>OpenApiConfig     — Swagger / OpenAPI 文档</li>
 *   <li>WebSocketConfig   — WebSocket 消息代理</li>
 *   <li>GlobalExceptionHandler — 全局异常处理</li>
 * </ul>
 * <p>
 * <strong>依赖约束</strong>
 * <p>
 * SecurityConfig 引用 auth 模块的安全组件（Filter、Provider、Converter），
 * 通过 Named Interface 声明：
 * <ul>
 *   <li>{@code auth::security} — LoginRateLimitFilter、认证 Provider/Converter（安全过滤链组件）</li>
 * </ul>
 * <p>
 * {@code common} 共享内核隐式允许，无需声明。
 * <p>
 * 业务模块（auth、system）禁止反向依赖 config（避免循环）。
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "全局配置模块",
        allowedDependencies = {"auth::security", "common"}
)
package com.eagle.system.config;

