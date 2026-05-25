package com.eagle.auth.interfaces.controller;

import com.eagle.common.exception.AppException;
import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.auth.infrastructure.config.WechatWebProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 登录控制器
 * <p>
 * 提供登录页面渲染和短信验证码 Web 登录端点。
 *
 * @author sunshixiong
 */
@Slf4j
@Tag(name = "登录页面/会话登录", description = "Thymeleaf 渲染的登录页与基于 Session 的短信登录端点(非 REST,Swagger 仅作文档)")
@Controller
@RequestMapping(value = "login")
@RequiredArgsConstructor
public class LoginController {

    private static final String STATE_SESSION_KEY = "WECHAT_OAUTH_STATE";
    private static final String SMS_LOGIN_ERROR_REDIRECT = "/login?error&sms";

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final WechatWebProperties wechatWebProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 渲染登录页面
     * <p>
     * 传递微信开放平台 PC 扫码配置，用于页面内嵌二维码。
     */
    @Operation(summary = "渲染登录页", description = "返回 Thymeleaf 模板 'login';微信 PC 扫码启用时携带 appId/state 内嵌二维码")
    @GetMapping
    public String login(Model model, HttpSession session) {
        boolean wechatEnabled = wechatWebProperties.isEnabled();
        model.addAttribute("wechatEnabled", wechatEnabled);
        if (wechatEnabled) {
            String state = UUID.randomUUID().toString();
            session.setAttribute(STATE_SESSION_KEY, state);
            WechatWebProperties.Pc pc = wechatWebProperties.getPc();
            model.addAttribute("wechatAppId", pc.getAppId());
            model.addAttribute("wechatRedirectUri", pc.getRedirectUri());
            model.addAttribute("wechatState", state);
        }
        return "login";
    }

    /**
     * 渲染找回密码页面
     */
    @Operation(summary = "渲染找回密码页", description = "返回 Thymeleaf 模板 'reset-password'")
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    /**
     * 渲染绑定手机号页面（微信登录后）
     *
     * @param accountId   当前登录账号 ID
     * @param redirectUrl 绑定/跳过后的重定向地址（通常为 OAuth2 授权端点）
     */
    @Operation(summary = "渲染绑定手机号页", description = "微信扫码后未绑定手机号时引导用户绑定;accountId+redirectUrl 用于绑定/跳过后跳回 OAuth2 流程")
    @GetMapping("/bind-phone")
    public String bindPhone(Model model,
                            @RequestParam Long accountId,
                            @RequestParam(defaultValue = "/") String redirectUrl) {
        model.addAttribute("accountId", accountId);
        model.addAttribute("redirectUrl", redirectUrl);
        return "bind-phone";
    }

    /**
     * 短信验证码 Web 登录（session-based）
     * <p>
     * 验证短信码后建立 session 认证，与微信 Web 登录采用相同的 session 认证模式。
     *
     * @param phone 手机号
     * @param code  短信验证码
     */
    @Operation(summary = "短信验证码 Web 登录(Session)",
            description = "校验验证码 → 建立 Session 认证;失败重定向 /login?error&sms,成功重定向到 SavedRequest 或 /。错误码:INVALID_PHONE_FORMAT、SMS_CODE_INVALID")
    @PostMapping("/sms")
    public void smsLogin(@RequestParam String phone,
                         @RequestParam String code,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        Account account = accountApplicationService.authenticateBySmsCode(phone, code);
        UserDetails userDetails = userDetailsService.loadUserByUsername(account.getUsername());

        // SAS 7.0 OIDC id_token 生成要求 Authentication 携带 FactorGrantedAuthority 以提供 auth_time；
        // 短信验证码属于一次性凭证（One-Time-Token）因子。
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(userDetails.getAuthorities());
        authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.OTT_AUTHORITY));

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // 手工 setAuthentication 不会触发 ProviderManager 的 AuthenticationSuccessEvent，
        // 导致 LoginAttemptService 失败计数不会被重置；此处显式发布，保持与表单登录行为一致。
        eventPublisher.publishEvent(new AuthenticationSuccessEvent(authToken));

        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = savedRequest != null ? savedRequest.getRedirectUrl() : "/";
        log.info("短信登录成功, redirect: {}", targetUrl);
        response.sendRedirect(targetUrl);
    }

    /**
     * Form-login 的失败重定向器：短信登录路径的业务异常统一回到 {@code /login?error&sms}。
     * <p>对应规范 05-api.md "Controller 禁止 try-catch"——@ExceptionHandler 是声明式处理，
     * 不属于 try-catch。其他端点的异常仍由全局 GlobalExceptionHandler 接管。</p>
     */
    @ExceptionHandler(AppException.class)
    public void handleSmsLoginFailure(AppException ex,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws IOException {
        if (!"/login/sms".equals(request.getRequestURI())) {
            throw ex;
        }
        log.warn("短信登录失败: code={}", ex.getErrorCode().getCode(), ex);
        response.sendRedirect(SMS_LOGIN_ERROR_REDIRECT);
    }
}