package com.eagle.payment;

import com.eagle.payment.util.PaymentSignatureValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link PaymentSignatureValidator} 单元测试。
 *
 * <p>验证工具类在签名不合法时的容错行为——不抛出异常，只返回 {@code false}。
 * 不使用真实密钥，仅测试异常分支的防御逻辑。
 */
@DisplayName("PaymentSignatureValidator")
class PaymentSignatureValidatorTest {

    @Nested
    @DisplayName("verifyAlipaySign")
    class VerifyAlipaySign {

        @Test
        @DisplayName("shouldReturnFalseForTamperedData — 使用无效公钥时返回 false 而不抛异常")
        void shouldReturnFalseForTamperedData() {
            Map<String, String> params = new HashMap<>();
            params.put("out_trade_no", "ORDER-001");
            params.put("total_amount", "99.00");
            params.put("sign", "INVALID_SIGNATURE");
            params.put("sign_type", "RSA2");

            // 使用任意伪造公钥
            String fakePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0000";

            boolean result = PaymentSignatureValidator.verifyAlipaySign(params, fakePublicKey, "RSA2");

            assertFalse(result, "无效公钥/签名数据时 verifyAlipaySign 应返回 false");
        }

        @Test
        @DisplayName("shouldReturnFalseWhenSignIsMissing — params 中缺少 sign 字段时返回 false")
        void shouldReturnFalseWhenSignIsMissing() {
            Map<String, String> params = new HashMap<>();
            params.put("out_trade_no", "ORDER-002");
            // 故意不放 sign 字段

            String fakePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0000";

            boolean result = PaymentSignatureValidator.verifyAlipaySign(params, fakePublicKey, "RSA2");

            assertFalse(result, "缺少 sign 字段时应返回 false");
        }

        @Test
        @DisplayName("shouldReturnFalseForEmptyParams — 空 params 时返回 false 而不抛异常")
        void shouldReturnFalseForEmptyParams() {
            boolean result = PaymentSignatureValidator.verifyAlipaySign(
                    Map.of(), "fakeKey", "RSA2");

            assertFalse(result, "空 params 时应返回 false");
        }
    }

    @Nested
    @DisplayName("verifyWechatSign (byte[] 重载)")
    class VerifyWechatSignBytes {

        @Test
        @DisplayName("shouldReturnFalseForInvalidPublicKeyBytes — 无效公钥字节返回 false 而不抛异常")
        void shouldReturnFalseForInvalidPublicKeyBytes() {
            byte[] invalidKeyBytes = "not-a-real-der-key".getBytes();
            String timestamp = "1700000000";
            String nonce = "random-nonce";
            String body = "{\"event_type\":\"TRANSACTION.SUCCESS\"}";
            String signature = Base64.getEncoder().encodeToString("fake-signature".getBytes());

            boolean result = PaymentSignatureValidator.verifyWechatSign(
                    invalidKeyBytes, timestamp, nonce, body, signature);

            assertFalse(result, "无效公钥字节时 verifyWechatSign 应返回 false");
        }

        @Test
        @DisplayName("shouldReturnFalseForInvalidBase64Signature — 非 Base64 签名返回 false 而不抛异常")
        void shouldReturnFalseForInvalidBase64Signature() {
            byte[] invalidKeyBytes = new byte[]{0x01, 0x02, 0x03};

            boolean result = PaymentSignatureValidator.verifyWechatSign(
                    invalidKeyBytes, "ts", "nonce", "body", "!!!notBase64!!!");

            assertFalse(result, "非 Base64 签名时应返回 false");
        }
    }

    @Nested
    @DisplayName("verifyWechatSign (String 重载)")
    class VerifyWechatSignString {

        @Test
        @DisplayName("shouldReturnFalseForInvalidSignature — 使用 Base64 伪造公钥时返回 false")
        void shouldReturnFalseForInvalidSignature() {
            // 用 Base64 编码的随机字节模拟无效公钥
            String fakePublicKeyBase64 = Base64.getEncoder().encodeToString(
                    "this-is-not-a-real-rsa-key".getBytes());
            String signature = Base64.getEncoder().encodeToString("fake-sig".getBytes());

            boolean result = PaymentSignatureValidator.verifyWechatSign(
                    fakePublicKeyBase64, "1700000000", "nonce123", "body", signature);

            assertFalse(result, "伪造公钥时 verifyWechatSign(String) 应返回 false");
        }

        @Test
        @DisplayName("shouldReturnFalseForTamperedBody — 正确结构但内容篡改时返回 false")
        void shouldReturnFalseForTamperedBody() {
            // 即使消息结构正确但签名与内容不匹配，也应返回 false
            String fakePublicKeyBase64 = Base64.getEncoder().encodeToString(
                    "fake-key-bytes".getBytes());
            String signature = Base64.getEncoder().encodeToString("tampered".getBytes());

            boolean result = PaymentSignatureValidator.verifyWechatSign(
                    fakePublicKeyBase64,
                    "1700000001",
                    "nonce-abc",
                    "{\"tampered\":true}",
                    signature);

            assertFalse(result, "篡改的 body 和签名不匹配时应返回 false");
        }
    }
}
