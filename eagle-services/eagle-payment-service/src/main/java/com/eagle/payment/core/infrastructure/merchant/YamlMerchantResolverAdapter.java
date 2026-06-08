package com.eagle.payment.core.infrastructure.merchant;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.port.MerchantResolverPort;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v1 单商户 / yml 凭证实现。
 *
 * <p>忽略 {@code tenantId},所有租户共用一套凭证(由 {@link PaymentProperties} 注入)。
 * v2 会替换为 {@code DatabaseMerchantResolverAdapter},按租户查 {@code t_payment_merchant}
 * 表;接口契约保持不变,无需调整 ApplicationService。
 *
 * <p>渠道返回 null 即表示该渠道未启用或未配置必要字段,Gateway 适配器据此抛
 * {@code CHANNEL_UNAVAILABLE}。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class YamlMerchantResolverAdapter implements MerchantResolverPort {

    private final PaymentProperties properties;

    @Override
    @Nullable
    public Map<String, String> resolve(String tenantId, PaymentChannel channel) {
        return switch (channel) {
            case ALIPAY -> resolveAlipay();
            case WECHAT -> resolveWechat();
        };
    }

    @Nullable
    private Map<String, String> resolveAlipay() {
        PaymentProperties.Alipay a = properties.getAlipay();
        if (!a.isEnabled() || a.getAppId().isEmpty() || a.getPrivateKey().isEmpty()
                || a.getAlipayPublicKey().isEmpty()) {
            return null;
        }
        Map<String, String> creds = new LinkedHashMap<>();
        creds.put("appId", a.getAppId());
        creds.put("gatewayUrl", a.getGatewayUrl());
        creds.put("signType", a.getSignType());
        creds.put("charset", a.getCharset());
        creds.put("format", a.getFormat());
        creds.put("privateKey", a.getPrivateKey());
        creds.put("alipayPublicKey", a.getAlipayPublicKey());
        creds.put("notifyBaseUrl", a.getNotifyBaseUrl());
        return creds;
    }

    @Nullable
    private Map<String, String> resolveWechat() {
        PaymentProperties.Wechat w = properties.getWechat();
        if (!w.isEnabled() || w.getAppId().isEmpty() || w.getMchId().isEmpty()
                || w.getApiV3Key().isEmpty() || w.getPrivateKey().isEmpty()
                || w.getPrivateKeySerialNo().isEmpty()) {
            return null;
        }
        Map<String, String> creds = new LinkedHashMap<>();
        creds.put("appId", w.getAppId());
        creds.put("mchId", w.getMchId());
        creds.put("apiV3Key", w.getApiV3Key());
        creds.put("privateKey", w.getPrivateKey());
        creds.put("privateKeySerialNo", w.getPrivateKeySerialNo());
        creds.put("notifyBaseUrl", w.getNotifyBaseUrl());
        return creds;
    }
}
