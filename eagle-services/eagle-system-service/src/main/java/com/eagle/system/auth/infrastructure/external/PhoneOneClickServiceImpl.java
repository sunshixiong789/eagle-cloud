package com.eagle.system.auth.infrastructure.external;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.GetMobileRequest;
import com.aliyun.dypnsapi20170525.models.GetMobileResponse;
import com.aliyun.dypnsapi20170525.models.GetMobileResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.common.util.LogMask;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.service.PhoneOneClickService;
import com.eagle.system.auth.infrastructure.config.PhoneOneClickProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 手机号一键登录服务实现
 * <p>
 * 当前提供 {@code mock} 与 {@code aliyun} 两种提供方：
 * <ul>
 *   <li>{@code mock}（默认）：access_token 直接当作手机号校验，仅用于开发联调</li>
 *   <li>{@code aliyun}：调用阿里云号码认证 dypnsapi 的 GetMobile 接口换取真实手机号</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneOneClickServiceImpl implements PhoneOneClickService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    /** 阿里云号码认证业务成功状态码 */
    private static final String ALIYUN_SUCCESS_CODE = "OK";

    private final PhoneOneClickProperties properties;

    private Client aliyunClient;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled() || !"aliyun".equalsIgnoreCase(properties.getProvider())) {
            return;
        }
        if (properties.getAccessKeyId().isBlank() || properties.getAccessKeySecret().isBlank()) {
            log.warn("一键登录 aliyun 提供方未配置 AccessKey，初始化跳过；调用时将拒绝请求");
            return;
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret())
                    .setEndpoint(properties.getEndpoint());
            this.aliyunClient = new Client(config);
            log.info("阿里云号码认证客户端初始化完成，endpoint={}", properties.getEndpoint());
        } catch (Exception ex) {
            log.error("阿里云号码认证客户端初始化失败", ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }
    }

    @Override
    public String verifyAndGetPhone(String accessToken) {
        if (!properties.isEnabled()) {
            throw AuthErrorCode.ONE_CLICK_PROVIDER_DISABLED.toServiceException();
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw AuthErrorCode.ONE_CLICK_TOKEN_REQUIRED.toDomainException();
        }

        String provider = properties.getProvider();
        if ("aliyun".equalsIgnoreCase(provider)) {
            return verifyByAliyun(accessToken);
        }
        return verifyByMock(accessToken);
    }

    private String verifyByMock(String accessToken) {
        if (!PHONE_PATTERN.matcher(accessToken).matches()) {
            log.warn("一键登录 mock 模式收到非法 token，无法解析为手机号");
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toDomainException();
        }
        log.warn("一键登录 mock 模式：access_token 直接作为手机号使用，仅供开发环境，phone={}", LogMask.phone(accessToken));
        return accessToken;
    }

    private String verifyByAliyun(String accessToken) {
        if (aliyunClient == null) {
            log.error("一键登录 aliyun 客户端未初始化，请检查 eagle.auth.one-click.* 配置");
            throw AuthErrorCode.ONE_CLICK_PROVIDER_DISABLED.toServiceException();
        }

        GetMobileResponseBody body;
        try {
            GetMobileRequest request = new GetMobileRequest().setAccessToken(accessToken);
            GetMobileResponse response = aliyunClient.getMobile(request);
            body = response == null ? null : response.getBody();
        } catch (Exception ex) {
            log.error("调用阿里云号码认证失败", ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }

        if (body == null) {
            log.error("阿里云号码认证响应为空");
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException();
        }
        if (!ALIYUN_SUCCESS_CODE.equals(body.getCode())) {
            log.error("阿里云号码认证业务失败，code={}, message={}, requestId={}",
                    body.getCode(), body.getMessage(), body.getRequestId());
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException();
        }

        GetMobileResponseBody.GetMobileResponseBodyGetMobileResultDTO result = body.getGetMobileResultDTO();
        if (result == null || result.getMobile() == null || result.getMobile().isBlank()) {
            log.error("阿里云号码认证返回手机号为空，requestId={}", body.getRequestId());
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toServiceException();
        }
        String mobile = result.getMobile().trim();
        if (!PHONE_PATTERN.matcher(mobile).matches()) {
            log.error("阿里云号码认证返回非法手机号格式，requestId={}", body.getRequestId());
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toServiceException();
        }
        return mobile;
    }
}
