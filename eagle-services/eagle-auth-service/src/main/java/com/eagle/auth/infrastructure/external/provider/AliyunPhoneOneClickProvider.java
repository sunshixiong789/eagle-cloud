package com.eagle.auth.infrastructure.external.provider;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.GetMobileRequest;
import com.aliyun.dypnsapi20170525.models.GetMobileResponse;
import com.aliyun.dypnsapi20170525.models.GetMobileResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.infrastructure.config.PhoneOneClickProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 阿里云号码认证 Provider：调用 dypnsapi 的 GetMobile 接口换取真实手机号
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunPhoneOneClickProvider implements PhoneOneClickProvider {

    public static final String NAME = "aliyun";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final String SUCCESS_CODE = "OK";

    private final PhoneOneClickProperties properties;

    private Client aliyunClient;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled() || !NAME.equalsIgnoreCase(properties.getProvider())) {
            return;
        }
        PhoneOneClickProperties.Aliyun cfg = properties.getAliyun();
        if (cfg.getAccessKeyId().isBlank() || cfg.getAccessKeySecret().isBlank()) {
            log.warn("一键登录 aliyun 提供方未配置 AccessKey，初始化跳过；调用时将拒绝请求");
            return;
        }
        try {
            Config config = new Config()
                    .setAccessKeyId(cfg.getAccessKeyId())
                    .setAccessKeySecret(cfg.getAccessKeySecret())
                    .setEndpoint(cfg.getEndpoint());
            this.aliyunClient = new Client(config);
            log.info("阿里云号码认证客户端初始化完成，endpoint={}", cfg.getEndpoint());
        } catch (Exception ex) {
            log.error("阿里云号码认证客户端初始化失败", ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String verifyAndGetPhone(String accessToken) {
        if (aliyunClient == null) {
            log.error("一键登录 aliyun 客户端未初始化，请检查 eagle.auth.one-click.aliyun.* 配置");
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
        if (!SUCCESS_CODE.equals(body.getCode())) {
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
