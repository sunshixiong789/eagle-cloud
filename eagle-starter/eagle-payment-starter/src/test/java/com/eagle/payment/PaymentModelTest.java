package com.eagle.payment;

import com.eagle.payment.model.PayRequest;
import com.eagle.payment.model.PayResult;
import com.eagle.payment.model.RefundRequest;
import com.eagle.payment.model.RefundResult;
import com.eagle.payment.model.TransferRequest;
import com.eagle.payment.model.TransferResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付模型（Builder / POJO）单元测试。
 *
 * <p>验证各 @Data + @Builder 模型的 Builder 构建、字段赋值及语义。
 */
@DisplayName("PaymentModel")
class PaymentModelTest {

    @Nested
    @DisplayName("PayRequest")
    class PayRequestTests {

        @Test
        @DisplayName("payRequest_shouldBuildCorrectly — 必填字段正确赋值")
        void payRequestShouldBuildCorrectly() {
            PayRequest request = PayRequest.builder()
                    .outTradeNo("ORDER-001")
                    .subject("测试商品")
                    .amount(new BigDecimal("99.00"))
                    .description("订单描述")
                    .openId("user-openid-abc")
                    .build();

            assertEquals("ORDER-001", request.getOutTradeNo());
            assertEquals("测试商品", request.getSubject());
            assertEquals(new BigDecimal("99.00"), request.getAmount());
            assertEquals("订单描述", request.getDescription());
            assertEquals("user-openid-abc", request.getOpenId());
        }

        @Test
        @DisplayName("payRequest_shouldUseDefaultExpireMinutes — 未指定时 expireMinutes 默认为 30")
        void payRequestShouldUseDefaultExpireMinutes() {
            PayRequest request = PayRequest.builder()
                    .outTradeNo("ORDER-002")
                    .amount(new BigDecimal("10.00"))
                    .build();

            assertEquals(30, request.getExpireMinutes(), "默认过期时间应为 30 分钟");
        }

        @Test
        @DisplayName("payRequest_shouldOverrideExpireMinutes — 显式指定 expireMinutes 时生效")
        void payRequestShouldOverrideExpireMinutes() {
            PayRequest request = PayRequest.builder()
                    .outTradeNo("ORDER-003")
                    .amount(BigDecimal.ONE)
                    .expireMinutes(60)
                    .build();

            assertEquals(60, request.getExpireMinutes());
        }
    }

    @Nested
    @DisplayName("PayResult")
    class PayResultTests {

        @Test
        @DisplayName("payResult_shouldMarkSuccess — success=true 时 payInfo 有值，errorMessage 为空")
        void payResultShouldMarkSuccess() {
            PayResult result = PayResult.builder()
                    .success(true)
                    .tradeNo("TXN-12345")
                    .outTradeNo("ORDER-001")
                    .payInfo("qr-code-url")
                    .build();

            assertTrue(result.isSuccess());
            assertNotNull(result.getPayInfo());
            assertNull(result.getErrorMessage(), "成功时 errorMessage 应为 null");
        }

        @Test
        @DisplayName("payResult_shouldMarkFailure — success=false 时 errorMessage 有值")
        void payResultShouldMarkFailure() {
            PayResult result = PayResult.builder()
                    .success(false)
                    .outTradeNo("ORDER-002")
                    .errorMessage("余额不足")
                    .build();

            assertFalse(result.isSuccess());
            assertEquals("余额不足", result.getErrorMessage());
            assertNull(result.getTradeNo(), "失败时 tradeNo 应为 null");
        }
    }

    @Nested
    @DisplayName("RefundRequest")
    class RefundRequestTests {

        @Test
        @DisplayName("refundRequest_shouldRequireTradeNo — outTradeNo / refundNo / refundAmount 正确赋值")
        void refundRequestShouldRequireTradeNo() {
            RefundRequest request = RefundRequest.builder()
                    .outTradeNo("ORDER-001")
                    .refundNo("REFUND-001")
                    .refundAmount(new BigDecimal("50.00"))
                    .reason("商品质量问题")
                    .build();

            assertEquals("ORDER-001", request.getOutTradeNo());
            assertEquals("REFUND-001", request.getRefundNo());
            assertEquals(new BigDecimal("50.00"), request.getRefundAmount());
            assertEquals("商品质量问题", request.getReason());
        }
    }

    @Nested
    @DisplayName("RefundResult")
    class RefundResultTests {

        @Test
        @DisplayName("refundResult_shouldMarkSuccess — success=true 时 refundNo 有值")
        void refundResultShouldMarkSuccess() {
            RefundResult result = RefundResult.builder()
                    .success(true)
                    .refundNo("REFUND-001")
                    .outTradeNo("ORDER-001")
                    .build();

            assertTrue(result.isSuccess());
            assertEquals("REFUND-001", result.getRefundNo());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("refundResult_shouldMarkFailure — success=false 时 errorMessage 有值")
        void refundResultShouldMarkFailure() {
            RefundResult result = RefundResult.builder()
                    .success(false)
                    .refundNo("REFUND-002")
                    .errorMessage("退款超时")
                    .build();

            assertFalse(result.isSuccess());
            assertEquals("退款超时", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("TransferRequest")
    class TransferRequestTests {

        @Test
        @DisplayName("transferRequest_shouldBuildCorrectly — 转账必填字段正确赋值")
        void transferRequestShouldBuildCorrectly() {
            TransferRequest request = TransferRequest.builder()
                    .outBizNo("TRANSFER-001")
                    .payeeAccount("13800138000")
                    .payeeName("张三")
                    .amount(new BigDecimal("200.00"))
                    .remark("提现")
                    .build();

            assertEquals("TRANSFER-001", request.getOutBizNo());
            assertEquals("13800138000", request.getPayeeAccount());
            assertEquals("张三", request.getPayeeName());
            assertEquals(new BigDecimal("200.00"), request.getAmount());
            assertEquals("提现", request.getRemark());
        }
    }

    @Nested
    @DisplayName("TransferResult")
    class TransferResultTests {

        @Test
        @DisplayName("transferResult_shouldContainOrderId — success=true 时 orderId 有值")
        void transferResultShouldContainOrderId() {
            TransferResult result = TransferResult.builder()
                    .success(true)
                    .orderId("3PL20240101001")
                    .outBizNo("TRANSFER-001")
                    .build();

            assertTrue(result.isSuccess());
            assertEquals("3PL20240101001", result.getOrderId());
            assertEquals("TRANSFER-001", result.getOutBizNo());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("transferResult_shouldMarkFailure — success=false 时 errorMessage 有值")
        void transferResultShouldMarkFailure() {
            TransferResult result = TransferResult.builder()
                    .success(false)
                    .outBizNo("TRANSFER-002")
                    .errorMessage("账户异常")
                    .build();

            assertFalse(result.isSuccess());
            assertEquals("账户异常", result.getErrorMessage());
            assertNull(result.getOrderId());
        }
    }
}
