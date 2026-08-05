package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.WechatService;
import com.eagle.auth.core.infrastructure.config.WechatMiniProgramProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 微信小程序服务实现
 * <p>
 * 用 Spring {@link RestClient} 直调 {@code sns/jscode2session}，与
 * {@link WechatWebServiceImpl} 同一套手写实现，不依赖 WxJava SDK
 * —— 小程序登录只用到这一个接口，为它引一条 5.1 MB 的依赖链不划算。
 *
 * <p><strong>为什么先取 String 再解析，而不是直接 {@code .body(Record.class)}</strong>：
 * 微信这个端点返回的 {@code Content-Type} 是 {@code text/plain}，Jackson 的消息转换器
 * 只声明支持 {@code application/json} / {@code application/*+json}，直接绑定会抛
 * {@code UnknownContentTypeException}。原 WxJava 实现是读字符串再解析，故此处保持同样行为。
 *
 * @author sunshixiong
 */
@Service
public class WechatMiniProgramServiceImpl implements WechatService {

    private static final Logger log = LoggerFactory.getLogger(WechatMiniProgramServiceImpl.class);

    private static final String JS_CODE_2_SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final WechatMiniProgramProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WechatMiniProgramServiceImpl(WechatMiniProgramProperties properties,
                                        @Qualifier("wechatRestClient") RestClient restClient,
                                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatUserInfo getUserInfo(String code) {
        String body;
        try {
            body = restClient.get()
                    .uri(JS_CODE_2_SESSION_URL, properties.getAppId(), properties.getAppSecret(), code)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.error("微信小程序 jscode2session 调用失败", ex);
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException(ex);
        }

        if (body == null || body.isBlank()) {
            log.error("微信小程序 jscode2session 响应为空");
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException();
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (JacksonException ex) {
            // 响应体含 session_key（会话密钥），禁止整体落日志，只记长度
            log.error("微信小程序 jscode2session 响应解析失败, length={}", body.length(), ex);
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException(ex);
        }

        // 微信约定：出错时返回 errcode/errmsg；成功时无 errcode，或 errcode=0
        int errcode = node.path("errcode").asInt(0);
        if (errcode != 0) {
            log.error("微信小程序 jscode2session 业务失败, errcode={}, errmsg={}",
                    errcode, node.path("errmsg").asString(""));
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException();
        }

        String openid = trimToNull(node.path("openid").asString(null));
        if (openid == null) {
            log.error("微信小程序 jscode2session 未返回 openid");
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException();
        }

        return new WechatUserInfo(
                openid,
                // unionid 仅在小程序已绑定开放平台账号时下发，允许为空
                trimToNull(node.path("unionid").asString(null)),
                trimToNull(node.path("session_key").asString(null)));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
