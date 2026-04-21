package com.eagle.system.auth.web.controller;

import com.eagle.system.auth.application.service.WechatWebUserService;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.service.WechatWebService;
import com.eagle.system.auth.domain.service.WechatWebService.WechatWebUserInfo;
import com.eagle.system.auth.infrastructure.config.WechatWebProperties;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 微信 Web 端登录控制器
 * <p>
 * 处理两种微信 Web 登录流程：
 * <ul>
 *   <li>PC 扫码登录：通过微信开放平台网站应用（scope: snsapi_login）</li>
 *   <li>H5 网页授权：通过微信公众号（scope: snsapi_userinfo），在微信内置浏览器使用</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/login/wechat")
public class WechatWebLoginController {

    private static final Logger log = LoggerFactory.getLogger(WechatWebLoginController.class);
    private static final String STATE_SESSION_KEY = "WECHAT_OAUTH_STATE";

    private final WechatWebService wechatWebService;
    private final WechatWebUserService wechatWebUserService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final WechatWebProperties wechatWebProperties;

    // ==================== PC 扫码登录 ====================

    /**
     * 发起 PC 微信扫码登录，重定向到微信开放平台授权页
     */
    @GetMapping("/pc")
    public void initiatePcLogin(HttpServletRequest request, HttpServletResponse response,
                                HttpSession session) throws IOException {
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_SESSION_KEY, state);
        WechatWebProperties.Pc pc = wechatWebProperties.getPc();
        String authUrl = "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + pc.getAppId()
                + "&redirect_uri=" + URLEncoder.encode(pc.getRedirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state;
        response.sendRedirect(authUrl);
    }

    /**
     * PC 微信扫码登录回调
     *
     * @param code  微信临时授权 code
     * @param state 随机 state（用于验证防 CSRF）
     */
    @GetMapping("/pc/callback")
    public void handlePcCallback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 HttpSession session) throws IOException {
        if (validateState(state, session, response)) {
            return;
        }
        WechatWebUserInfo userInfo = wechatWebService.exchangePcCode(code);
        authenticateAndRedirect(userInfo, request, response);
    }

    // ==================== H5 公众号授权 ====================

    /**
     * 发起 H5 微信公众号网页授权，重定向到微信授权页
     */
    @GetMapping("/h5")
    public void initiateH5Login(HttpServletRequest request, HttpServletResponse response,
                                HttpSession session) throws IOException {
        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_SESSION_KEY, state);
        WechatWebProperties.H5 h5 = wechatWebProperties.getH5();
        // #wechat_redirect 必须追加在 URL-encode 后的 redirect_uri 之后
        String authUrl = "https://open.weixin.qq.com/connect/oauth2/authorize"
                + "?appid=" + h5.getAppId()
                + "&redirect_uri=" + URLEncoder.encode(h5.getRedirectUri(), StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=snsapi_userinfo"
                + "&state=" + state
                + "#wechat_redirect";
        response.sendRedirect(authUrl);
    }

    /**
     * H5 公众号授权登录回调
     *
     * @param code  微信临时授权 code
     * @param state 随机 state（用于验证防 CSRF）
     */
    @GetMapping("/h5/callback")
    public void handleH5Callback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 HttpSession session) throws IOException {
        if (validateState(state, session, response)) {
            return;
        }
        WechatWebUserInfo userInfo = wechatWebService.exchangeH5Code(code);
        authenticateAndRedirect(userInfo, request, response);
    }

    // ==================== 内部方法 ====================

    /**
     * 验证 state 参数防止 CSRF 攻击
     *
     * @return true 表示验证通过，false 表示验证失败（已写响应）
     */
    private boolean validateState(String state, HttpSession session,
                                  HttpServletResponse response) throws IOException {
        String storedState = (String) session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);
        if (storedState == null || !storedState.equals(state)) {
            log.warn("微信 Web 登录 state 验证失败，可能存在 CSRF 攻击");
            response.sendRedirect("/login?error");
            return true;
        }
        return false;
    }

    /**
     * 完成认证并重定向
     */
    private void authenticateAndRedirect(WechatWebUserInfo info,
                                         HttpServletRequest request,
                                         HttpServletResponse response) throws IOException {
        try {
            Account account = wechatWebUserService.findOrCreateWechatWebAccount(info);
            UserDetails userDetails = userDetailsService.loadUserByUsername(account.getUsername());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            log.info("微信 Web 登录成功, username: {}, channel: {}",
                    account.getUsername(), info.channel());

            // 获取原始请求地址（通常是 /oauth2/authorize?...）
            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String targetUrl = savedRequest != null ? savedRequest.getRedirectUrl() : "/";

            // 账号未绑定手机号，引导绑定（携带原始重定向地址）
            if (account.getPhone() == null || account.getPhone().isBlank()) {
                String bindUrl = "/login/bind-phone?accountId=" + account.getId()
                        + "&redirectUrl=" + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);
                response.sendRedirect(bindUrl);
                return;
            }

            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("微信 Web 登录失败, channel: {}, error: {}", info.channel(), e.getMessage(), e);
            response.sendRedirect("/login?error");
        }
    }
}
