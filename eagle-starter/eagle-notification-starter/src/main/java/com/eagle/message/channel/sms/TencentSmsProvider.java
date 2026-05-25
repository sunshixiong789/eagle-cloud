package com.eagle.message.channel.sms;

import com.eagle.message.properties.MessageProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 腾讯云短信服务商实现。
 *
 * <p>腾讯云在服务端完成模板渲染，模板参数按 <strong>顺序</strong>展开为字符串数组；
 * 模板 ID 为数字字符串（如 {@code 100001}）。
 *
 * <p>注意：手机号必须为 E.164 格式，国内号需带 {@code +86} 前缀。
 * 本实现会自动为以 {@code 1} 开头的 11 位号码补上 {@code +86}，其它情况按原样传递。
 *
 * @author eagle
 */
@Slf4j
public class TencentSmsProvider implements SmsProvider {

    public static final String NAME = "tencent";

    /**
     * 腾讯云 SMS API 默认 Endpoint。
     */
    private static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";

    private final SmsClient client;
    private final String sdkAppId;

    public TencentSmsProvider(MessageProperties properties) {
        MessageProperties.Sms sms = properties.getSms();
        this.sdkAppId = sms.getSdkAppId();
        if (this.sdkAppId == null || this.sdkAppId.isBlank()) {
            throw new IllegalStateException(
                    "Tencent SMS sdk-app-id is required (eagle.message.sms.sdk-app-id)");
        }

        try {
            Credential cred = new Credential(sms.getAccessKeyId(), sms.getAccessKeySecret());
            HttpProfile httpProfile = new HttpProfile();
            String endpoint = sms.getEndpoint();
            if (endpoint == null || endpoint.isBlank()
                    || "dysmsapi.aliyuncs.com".equals(endpoint)) {
                // 用户未改 endpoint 时使用腾讯云默认值，避免拿到阿里云的默认串
                endpoint = DEFAULT_ENDPOINT;
            }
            httpProfile.setEndpoint(endpoint);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            this.client = new SmsClient(cred, sms.getRegion(), clientProfile);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Tencent SMS client", e);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void send(String phone, String templateId, String signName, Map<String, String> params) {
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(sdkAppId);
            request.setSignName(signName);
            request.setTemplateId(templateId);
            request.setPhoneNumberSet(new String[]{normalizePhone(phone)});
            request.setTemplateParamSet(toOrderedParams(params));

            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statuses = response.getSendStatusSet();
            if (statuses == null || statuses.length == 0) {
                log.error("Tencent SMS empty response: phone={}", phone);
                throw new RuntimeException("Tencent SMS empty response");
            }
            SendStatus status = statuses[0];
            if (!"Ok".equalsIgnoreCase(status.getCode())) {
                String err = "Tencent SMS failed: " + status.getMessage();
                log.error("Tencent SMS send failed: phone={}, code={}, message={}",
                        phone, status.getCode(), status.getMessage());
                throw new RuntimeException(err);
            }
            log.info("Tencent SMS sent to {}, serialNo={}", phone, status.getSerialNo());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tencent SMS send error: phone={}", phone, e);
            throw new RuntimeException("Tencent SMS send error", e);
        }
    }

    /**
     * 国内 11 位手机号补 {@code +86}；已带 {@code +} 或非 11 位（含国际号、靓号）原样返回。
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        if (trimmed.length() == 11 && trimmed.startsWith("1")) {
            return "+86" + trimmed;
        }
        return trimmed;
    }

    /**
     * 将参数 Map 转为腾讯云要求的有序字符串数组。
     * <p>调用方需使用 {@link java.util.LinkedHashMap} 或确保 Map 的迭代顺序与模板占位符顺序一致。
     */
    private String[] toOrderedParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return new String[0];
        }
        String[] arr = new String[params.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            arr[i++] = entry.getValue() == null ? "" : entry.getValue();
        }
        return arr;
    }
}
