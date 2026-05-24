package com.eagle.auth.infrastructure.external.provider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.eagle.auth.domain.AuthErrorCode;
import com.eagle.auth.infrastructure.config.PhoneOneClickProperties;
import com.tencentcloudapi.common.CommonClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 腾讯云号码认证 Provider：通过 SDK {@link CommonClient} 调用号码认证服务 (PNSV) 的 GetPhoneNumber 接口
 * <p>
 * 走 CommonClient 而非具体产品 SDK 的好处：不锁死 service/version/action，
 * 公司若开通的是别的腾讯云号码认证产品（或同产品后续升版本），调整 yaml 即可。
 * 默认值按腾讯云号码认证服务（PNSV）惯例预设，按控制台实际开通服务核对：
 * <pre>
 *   eagle.auth.one-click.tencent.service  = pnsv
 *   eagle.auth.one-click.tencent.version  = 2018-07-11
 *   eagle.auth.one-click.tencent.action   = GetPhoneNumber
 * </pre>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TencentPhoneOneClickProvider implements PhoneOneClickProvider {

    public static final String NAME = "tencent";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final PhoneOneClickProperties properties;

    private CommonClient tencentClient;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled() || !NAME.equalsIgnoreCase(properties.getProvider())) {
            return;
        }
        PhoneOneClickProperties.Tencent cfg = properties.getTencent();
        if (cfg.getSecretId().isBlank() || cfg.getSecretKey().isBlank()) {
            log.warn("一键登录 tencent 提供方未配置 SecretId/SecretKey，初始化跳过；调用时将拒绝请求");
            return;
        }
        try {
            Credential credential = new Credential(cfg.getSecretId(), cfg.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            if (!cfg.getEndpoint().isBlank()) {
                httpProfile.setEndpoint(cfg.getEndpoint());
            }
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            this.tencentClient = new CommonClient(
                    cfg.getService(), cfg.getVersion(), credential, cfg.getRegion(), clientProfile);
            log.info("腾讯云号码认证客户端初始化完成，service={}, version={}, action={}, region={}",
                    cfg.getService(), cfg.getVersion(), cfg.getAction(), cfg.getRegion());
        } catch (Exception ex) {
            log.error("腾讯云号码认证客户端初始化失败", ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String verifyAndGetPhone(String accessToken) {
        if (tencentClient == null) {
            log.error("一键登录 tencent 客户端未初始化，请检查 eagle.auth.one-click.tencent.* 配置");
            throw AuthErrorCode.ONE_CLICK_PROVIDER_DISABLED.toServiceException();
        }

        PhoneOneClickProperties.Tencent cfg = properties.getTencent();
        String responseJson;
        try {
            JSONObject params = new JSONObject();
            params.put("AccessToken", accessToken);
            responseJson = tencentClient.call(cfg.getAction(), params.toJSONString());
        } catch (TencentCloudSDKException ex) {
            log.error("调用腾讯云号码认证失败，action={}, sdkErrorCode={}",
                    cfg.getAction(), ex.getErrorCode(), ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        } catch (Exception ex) {
            log.error("调用腾讯云号码认证发生未预期异常", ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }

        if (responseJson == null || responseJson.isBlank()) {
            log.error("腾讯云号码认证响应为空");
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException();
        }

        JSONObject root;
        try {
            root = JSON.parseObject(responseJson);
        } catch (Exception ex) {
            log.error("腾讯云号码认证响应解析失败，raw={}", responseJson, ex);
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException(ex);
        }
        // CommonClient.call() 已剥离最外层 {"Response": {...}}，但兼容两种情况
        JSONObject body = root.containsKey("Response") ? root.getJSONObject("Response") : root;

        JSONObject error = body.getJSONObject("Error");
        if (error != null) {
            log.error("腾讯云号码认证业务失败，code={}, message={}, requestId={}",
                    error.getString("Code"), error.getString("Message"), body.getString("RequestId"));
            throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException();
        }

        // 部分产品在 Response 中携带 Code/Status 字段，按配置校验
        String successCode = cfg.getSuccessCode();
        if (successCode != null && !successCode.isBlank()) {
            String code = body.getString("Code");
            if (code == null) {
                code = body.getString("Status");
            }
            if (code != null && !successCode.equalsIgnoreCase(code)) {
                log.error("腾讯云号码认证业务失败，code={}, message={}, requestId={}",
                        code, body.getString("Message"), body.getString("RequestId"));
                throw AuthErrorCode.ONE_CLICK_VERIFY_FAILED.toServiceException();
            }
        }

        String mobile = body.getString(cfg.getPhoneField());
        if (mobile == null || mobile.isBlank()) {
            log.error("腾讯云号码认证返回手机号为空，requestId={}", body.getString("RequestId"));
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toServiceException();
        }
        mobile = mobile.trim();
        // 部分场景手机号带 +86 前缀
        if (mobile.startsWith("+86")) {
            mobile = mobile.substring(3);
        }
        if (!PHONE_PATTERN.matcher(mobile).matches()) {
            log.error("腾讯云号码认证返回非法手机号格式，requestId={}", body.getString("RequestId"));
            throw AuthErrorCode.ONE_CLICK_PHONE_PARSE_FAILED.toServiceException();
        }
        return mobile;
    }
}
