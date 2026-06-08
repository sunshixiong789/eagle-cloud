package com.eagle.idgenerator.generator;

import com.eagle.idgenerator.properties.IdGeneratorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OrderNoGenerator} 单元测试。
 */
@DisplayName("OrderNoGenerator")
class OrderNoGeneratorTest {

    private static final String PREFIX_ORD = "ORD";
    private static final String PREFIX_PAY = "PAY";
    private static final String PREFIX_RFD = "RFD";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNoGenerator orderNoGenerator;

    @BeforeEach
    void setUp() {
        IdGeneratorProperties props = new IdGeneratorProperties();
        props.setWorkerId(1);
        props.setDatacenterId(1);
        props.setSequence(0);
        SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(props);
        orderNoGenerator = new OrderNoGenerator(snowflake);
    }

    @Nested
    @DisplayName("generate(prefix)")
    class GenerateWithPrefix {

        @Test
        @DisplayName("使用前缀时应生成排序号")
        void shouldGenerateOrderNoWithPrefix() {
            String orderNo = orderNoGenerator.generate(PREFIX_ORD);

            assertNotNull(orderNo);
            assertTrue(orderNo.startsWith(PREFIX_ORD),
                    "Order no should start with '" + PREFIX_ORD + "' but was: " + orderNo);
            // Format: prefix(3) + date(8) + seq(9) = 20 chars minimum
            assertTrue(orderNo.length() >= PREFIX_ORD.length() + 8 + 9,
                    "Order no length should be at least " + (PREFIX_ORD.length() + 17) + " but was: " + orderNo.length());
        }

        @Test
        @DisplayName("应包含当天日期")
        void shouldEmbedTodaysDate() {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String orderNo = orderNoGenerator.generate(PREFIX_ORD);

            assertTrue(orderNo.contains(today),
                    "Order no '" + orderNo + "' should contain today's date '" + today + "'");
        }

        @Test
        @DisplayName("应生成支付无")
        void shouldGeneratePayNo() {
            String payNo = orderNoGenerator.generate(PREFIX_PAY);

            assertNotNull(payNo);
            assertTrue(payNo.startsWith(PREFIX_PAY),
                    "Pay no should start with '" + PREFIX_PAY + "' but was: " + payNo);
        }

        @Test
        @DisplayName("应生成退款无")
        void shouldGenerateRefundNo() {
            String refundNo = orderNoGenerator.generate(PREFIX_RFD);

            assertNotNull(refundNo);
            assertTrue(refundNo.startsWith(PREFIX_RFD),
                    "Refund no should start with '" + PREFIX_RFD + "' but was: " + refundNo);
        }

        @Test
        @DisplayName("使用空前缀时应生成无")
        void shouldGenerateNoWithEmptyPrefix() {
            String orderNo = orderNoGenerator.generate("");

            assertNotNull(orderNo);
            // Format: date(8) + seq(9) = 17 chars
            assertEquals(17, orderNo.length(),
                    "No-prefix order no should have length 17 but was: " + orderNo.length());
        }

        @Test
        @DisplayName("使用null 前缀时应生成无")
        void shouldGenerateNoWithNullPrefix() {
            String orderNo = orderNoGenerator.generate(null);

            assertNotNull(orderNo);
            assertEquals(17, orderNo.length(),
                    "Null-prefix order no should have length 17 but was: " + orderNo.length());
        }
    }

    @Nested
    @DisplayName("generate()")
    class GenerateNoArgs {

        @Test
        @DisplayName("使用out 前缀时应生成排序号")
        void shouldGenerateOrderNoWithoutPrefix() {
            String orderNo = orderNoGenerator.generate();

            assertNotNull(orderNo);
            assertEquals(17, orderNo.length(),
                    "No-arg order no should have length 17 but was: " + orderNo.length());
        }
    }
}
