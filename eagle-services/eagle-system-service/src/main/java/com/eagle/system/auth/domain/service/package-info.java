/**
 * Auth 模块领域服务接口（Named Interface "domain-services"）
 * <p>
 * 包含 auth 模块依赖的外部领域服务抽象，实现在 infrastructure 层：
 * <ul>
 *   <li>{@code WechatService}   — 微信小程序登录服务接口</li>
 *   <li>{@code WechatWebService}— 微信 PC 扫码登录服务接口</li>
 *   <li>{@code SmsService}      — 短信验证码服务接口</li>
 *   <li>{@code PasswordEncryptor} — 密码加密服务接口</li>
 * </ul>
 * <p>
 * 此包被声明为 Named Interface，允许 {@code config} 模块的 SecurityConfig
 * 通过接口引用注入具体实现（如 SmsServiceImpl、WechatMiniProgramServiceImpl）。
 *
 * @author sunshixiong
 */
@NamedInterface("domain-services")
package com.eagle.system.auth.domain.service;

import org.springframework.modulith.NamedInterface;