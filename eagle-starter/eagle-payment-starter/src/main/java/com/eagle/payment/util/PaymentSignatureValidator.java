package com.eagle.payment.util;

import com.alipay.api.internal.util.AlipaySignature;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 支付回调签名验证工具。
 *
 * <p>将验签逻辑从 Controller 和 Gateway 中剥离，便于单独测试和复用。
 * 此类设计为无状态工具类，所有方法均为静态方法。
 *
 * <p>依赖说明：
 * <ul>
 *   <li>{@link #verifyAlipaySign} — 依赖 {@code alipay-sdk-java}（compileOnly），
 *       需消费方在类路径引入支付宝 SDK</li>
 *   <li>{@link #verifyWechatSign} — 依赖 {@code wechatpay-java}（compileOnly），
 *       需消费方在类路径引入微信支付 SDK；
 *       亦可在无 SDK 时自行实现 SHA256withRSA 验签逻辑替换此方法</li>
 * </ul>
 *
 * @author eagle
 */
@Slf4j
public final class PaymentSignatureValidator {

    /** 工具类，禁止实例化 */
    private PaymentSignatureValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 验证支付宝回调签名。
     *
     * <p>使用 {@link AlipaySignature#rsaCheckV1} 完成 RSA/RSA2 验签。
     * 验签所需的公钥、字符集、签名类型由调用方传入，不在此工具类中持有状态。
     *
     * @param params    请求参数 Map（来自 HTTP 表单，已 URL 解码）
     * @param publicKey 支付宝公钥（Base64 编码的 X.509 公钥字符串）
     * @param signType  签名类型，通常为 {@code "RSA2"}
     * @return {@code true} 表示验签通过，{@code false} 表示验签失败
     */
    public static boolean verifyAlipaySign(Map<String, String> params,
                                           String publicKey,
                                           String signType) {
        try {
            return AlipaySignature.rsaCheckV1(params, publicKey, StandardCharsets.UTF_8.name(), signType);
        } catch (Exception e) {
            log.warn("[Payment] Alipay signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证微信支付 APIv3 回调签名。
     *
     * <p>微信 APIv3 使用 SHA256withRSA 签名，待验签消息格式为：
     * <pre>
     * {timestamp}\n{nonce}\n{body}\n
     * </pre>
     *
     * <p><b>注意：</b>此方法直接使用 JDK 内置 {@code java.security} 实现验签，
     * 不依赖微信支付 SDK，但需要调用方传入已解码的平台证书公钥字节数组。
     * 若使用微信支付 SDK 的 {@code NotificationParser}，SDK 已内部完成验签，
     * 无需额外调用此方法。
     *
     * @param publicKeyBytes 微信支付平台证书公钥字节数组（X.509 DER 格式）
     * @param timestamp      Wechatpay-Timestamp 头部值
     * @param nonce          Wechatpay-Nonce 头部值
     * @param body           原始请求体字符串
     * @param signature      Wechatpay-Signature 头部值（Base64 编码）
     * @return {@code true} 表示验签通过，{@code false} 表示验签失败
     */
    public static boolean verifyWechatSign(byte[] publicKeyBytes,
                                           String timestamp,
                                           String nonce,
                                           String body,
                                           String signature) {
        try {
            // 构造待验签消息
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";

            // 从 DER 格式字节数组重建公钥
            java.security.spec.X509EncodedKeySpec keySpec =
                    new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // SHA256withRSA 验签
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));

            byte[] signBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signBytes);
        } catch (Exception e) {
            log.warn("[Payment] Wechat signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证微信支付 APIv3 回调签名（便捷方法，接受 Base64 编码的公钥字符串）。
     *
     * <p>若平台证书公钥以 Base64 字符串形式存储，可使用此重载方法，
     * 内部自动解码后转交 {@link #verifyWechatSign(byte[], String, String, String, String)}。
     *
     * @param publicKeyBase64 Base64 编码的微信支付平台证书公钥
     * @param timestamp       Wechatpay-Timestamp 头部值
     * @param nonce           Wechatpay-Nonce 头部值
     * @param body            原始请求体字符串
     * @param signature       Wechatpay-Signature 头部值（Base64 编码）
     * @return {@code true} 表示验签通过，{@code false} 表示验签失败
     */
    public static boolean verifyWechatSign(String publicKeyBase64,
                                           String timestamp,
                                           String nonce,
                                           String body,
                                           String signature) {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        return verifyWechatSign(publicKeyBytes, timestamp, nonce, body, signature);
    }
}
