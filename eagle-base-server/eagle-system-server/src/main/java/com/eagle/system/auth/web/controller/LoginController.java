package com.eagle.system.auth.web.controller;

import com.eagle.auth.application.service.AccountApplicationService;
import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.auth.infrastructure.config.WechatWebProperties;
import com.eagle.common.exception.codes.AuthErrorCode;
import com.eagle.common.exception.codes.DataErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.UUID;

/**
 * 登录控制器
 * <p>
 * 提供登录页面渲染和短信验证码 Web 登录端点。
 *
 * @author sunshixiong
 */
@Controller
@RequestMapping(value = "login")
@RequiredArgsConstructor
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String STATE_SESSION_KEY = "WECHAT_OAUTH_STATE";

    private final SmsService smsService;
    private final AccountApplicationService accountApplicationService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final WechatWebProperties wechatWebProperties;

    /**
     * 渲染登录页面
     * <p>
     * 传递微信开放平台 PC 扫码配置，用于页面内嵌二维码。
     */
    @GetMapping
    public String login(Model model, HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_SESSION_KEY, state);
        WechatWebProperties.Pc pc = wechatWebProperties.getPc();
        model.addAttribute("wechatAppId", pc.getAppId());
        model.addAttribute("wechatRedirectUri", pc.getRedirectUri());
        model.addAttribute("wechatState", state);
        return "login";
    }

    /**
     * 渲染找回密码页面
     */
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
    @PostMapping("/sms")
    public void smsLogin(@RequestParam String phone,
                         @RequestParam String code,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        try {
            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                throw DataErrorCode.INVALID_PHONE_FORMAT.toDomainException();
            }
            if (!smsService.verifyCode(phone, code)) {
                throw AuthErrorCode.SMS_CODE_INVALID.toDomainException();
            }
            Account account = accountApplicationService.findOrCreateByPhone(phone);
            UserDetails userDetails = userDetailsService.loadUserByUsername(account.getUsername());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String targetUrl = savedRequest != null ? savedRequest.getRedirectUrl() : "/";
            log.info("短信登录成功, phone: {}, redirect: {}", phone, targetUrl);
            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("短信登录失败, phone: {}, error: {}", phone, e.getMessage(), e);
            response.sendRedirect("/login?error&sms");
        }
    }
}