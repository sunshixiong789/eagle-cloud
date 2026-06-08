package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 商户凭证解析端口。
 *
 * <p>v1 单商户实现 ({@code YamlMerchantResolverAdapter}) 从 yml 配置读取全局凭证。
 *
 * <p>返回值是 Map 形式以容纳不同渠道字段:
 * <ul>
 *   <li>{@link PaymentChannel#ALIPAY}: appId / privateKey / alipayPublicKey /
 *       gatewayUrl / signType / charset / format / notifyBaseUrl</li>
 *   <li>{@link PaymentChannel#WECHAT}: appId / mchId / apiV3Key / privateKey /
 *       privateKeySerialNo / notifyBaseUrl</li>
 * </ul>
 *
 * @author sunshixiong
 */
public interface MerchantResolverPort {

    /**
     * @param channel 渠道
     * @return 凭证 Map;{@code null} 表示该渠道未配置 (调用方按未开启处理)
     */
    @Nullable
    Map<String, String> resolve(PaymentChannel channel);
}
