package com.eagle.payment;

import com.eagle.payment.model.PaymentChannelEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PaymentChannelEnum} 单元测试。
 *
 * <p>纯枚举逻辑，无需 mock 或 Spring 上下文。
 */
@DisplayName("PaymentChannelEnum")
class PaymentChannelEnumTest {

    @Nested
    @DisplayName("fromCode")
    class FromCode {

        @Test
        @DisplayName("shouldFindByCodeLowerCase — 小写 'alipay' 可找到 ALIPAY")
        void shouldFindByCodeLowerCase() {
            PaymentChannelEnum result = PaymentChannelEnum.fromCode("alipay");
            assertEquals(PaymentChannelEnum.ALIPAY, result);
        }

        @Test
        @DisplayName("shouldFindByCodeUpperCase — 大写 'ALIPAY' 可找到 ALIPAY（大小写不敏感）")
        void shouldFindByCodeUpperCase() {
            PaymentChannelEnum result = PaymentChannelEnum.fromCode("ALIPAY");
            assertEquals(PaymentChannelEnum.ALIPAY, result);
        }

        @Test
        @DisplayName("shouldFindWechatByCode — 'wechat' 可找到 WECHAT")
        void shouldFindWechatByCode() {
            PaymentChannelEnum result = PaymentChannelEnum.fromCode("wechat");
            assertEquals(PaymentChannelEnum.WECHAT, result);
        }

        @Test
        @DisplayName("shouldFindBankCardByCode — 'bank_card' 可找到 BANK_CARD")
        void shouldFindBankCardByCode() {
            PaymentChannelEnum result = PaymentChannelEnum.fromCode("bank_card");
            assertEquals(PaymentChannelEnum.BANK_CARD, result);
        }

        @Test
        @DisplayName("shouldReturnThrowForUnknownCode — 未知 code 抛出 IllegalArgumentException")
        void shouldReturnThrowForUnknownCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> PaymentChannelEnum.fromCode("unknown_channel"),
                    "未知渠道编码应抛出 IllegalArgumentException");
        }

        @Test
        @DisplayName("shouldThrowForNullCode — null code 抛出 IllegalArgumentException")
        void shouldThrowForNullCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> PaymentChannelEnum.fromCode(null),
                    "null code 应抛出 IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("枚举字段完整性")
    class EnumFieldIntegrity {

        @Test
        @DisplayName("allEnumValuesShouldHaveNonBlankCode — 所有枚举值的 code 字段非空")
        void allEnumValuesShouldHaveNonBlankCode() {
            for (PaymentChannelEnum channel : PaymentChannelEnum.values()) {
                assertNotNull(channel.getCode(),
                        "枚举值 " + channel.name() + " 的 code 不应为 null");
                assertTrue(!channel.getCode().isBlank(),
                        "枚举值 " + channel.name() + " 的 code 不应为空白字符串");
            }
        }

        @Test
        @DisplayName("allEnumValuesShouldHaveNonBlankName — 所有枚举值的 name 字段非空")
        void allEnumValuesShouldHaveNonBlankName() {
            for (PaymentChannelEnum channel : PaymentChannelEnum.values()) {
                assertNotNull(channel.getName(),
                        "枚举值 " + channel.name() + " 的 name 不应为 null");
                assertTrue(!channel.getName().isBlank(),
                        "枚举值 " + channel.name() + " 的 name 不应为空白字符串");
            }
        }

        @Test
        @DisplayName("fromCodeShouldBeRoundTripConsistent — getCode() 再 fromCode() 应返回自身")
        void fromCodeShouldBeRoundTripConsistent() {
            for (PaymentChannelEnum channel : PaymentChannelEnum.values()) {
                PaymentChannelEnum found = PaymentChannelEnum.fromCode(channel.getCode());
                assertEquals(channel, found,
                        "fromCode(getCode()) 应返回自身，失败: " + channel.name());
            }
        }
    }
}
