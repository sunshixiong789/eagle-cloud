package com.eagle.system.auth.infrastructure.external;

import com.eagle.system.auth.domain.service.WechatWebService;
import com.eagle.system.auth.infrastructure.config.WechatWebProperties;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 微信 Web 端 OAuth2 服务实现
 * <p>
 * 使用 Spring {@link RestClient} 直接调用微信 API，无需额外 SDK 依赖。
 * <p>
 * 涉及的微信 API：
 * <ul>
 *   <li>access_token 换取：{@code https://api.weixin.qq.com/sns/oauth2/access_token}</li>
 *   <li>用户信息获取：{@code https://api.weixin.qq.com/sns/userinfo}</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Service
public class WechatWebServiceImpl implements WechatWebService {

    private static final Logger log = LoggerFactory.getLogger(WechatWebServiceImpl.class);

    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid={appId}&secret={secret}&code={code}&grant_type=authorization_code";
    private static final String USER_INFO_URL =
            "https://api.weixin.qq.com/sns/userinfo?access_token={accessToken}&openid={openid}&lang=zh_CN";

    private final WechatWebProperties wechatWebProperties;
    private final RestClient restClient;

    public WechatWebServiceImpl(WechatWebProperties wechatWebProperties,
                                @Qualifier("wechatRestClient") RestClient restClient) {
        this.wechatWebProperties = wechatWebProperties;
        this.restClient = restClient;
    }

    @Override
    public WechatWebUserInfo exchangePcCode(String code) {
        WechatWebProperties.Pc pc = wechatWebProperties.getPc();
        return exchange(pc.getAppId(), pc.getAppSecret(), code, "pc");
    }

    @Override
    public WechatWebUserInfo exchangeH5Code(String code) {
        WechatWebProperties.H5 h5 = wechatWebProperties.getH5();
        return exchange(h5.getAppId(), h5.getAppSecret(), code, "h5");
    }

    private WechatWebUserInfo exchange(String appId, String appSecret, String code, String channel) {
        // 1. 换取 access_token
        WechatTokenResponse tokenResponse = restClient.get()
                .uri(ACCESS_TOKEN_URL, appId, appSecret, code)
                .retrieve()
                .body(WechatTokenResponse.class);

        if (tokenResponse == null || tokenResponse.errcode() != null) {
            String errMsg = tokenResponse != null ? tokenResponse.errmsg() : "null response";
            log.error("微信 Web OAuth2 换取 access_token 失败, channel: {}, error: {}", channel, errMsg);
            throw AuthErrorCode.WECHAT_LOGIN_FAILED.toServiceException();
        }

        // 2. 换取用户信息
        WechatUserInfoResponse userInfo = restClient.get()
                .uri(USER_INFO_URL, tokenResponse.accessToken(), tokenResponse.openid())
                .retrieve()
                .body(WechatUserInfoResponse.class);

        if (userInfo == null || userInfo.errcode() != null) {
            String errMsg = userInfo != null ? userInfo.errmsg() : "null response";
            log.error("微信 Web OAuth2 获取用户信息失败, channel: {}, error: {}", channel, errMsg);
            throw AuthErrorCode.WECHAT_USER_INFO_FAILED.toServiceException();
        }

        return new WechatWebUserInfo(
                userInfo.openid(),
                userInfo.unionid(),
                userInfo.nickname(),
                userInfo.headimgurl(),
                channel
        );
    }

    /**
     * 微信 access_token 响应（access_token 正常 + errcode 错误二选一）
     */
    private record WechatTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("openid") String openid,
            @JsonProperty("unionid") String unionid,
            @JsonProperty("errcode") Integer errcode,
            @JsonProperty("errmsg") String errmsg
    ) {
    }

    /**
     * 微信用户信息响应
     */
    private record WechatUserInfoResponse(
            @JsonProperty("openid") String openid,
            @JsonProperty("unionid") String unionid,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("headimgurl") String headimgurl,
            @JsonProperty("errcode") Integer errcode,
            @JsonProperty("errmsg") String errmsg
    ) {
    }
}
