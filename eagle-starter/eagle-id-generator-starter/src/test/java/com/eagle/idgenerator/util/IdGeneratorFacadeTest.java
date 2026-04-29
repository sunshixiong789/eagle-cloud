package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IdGeneratorFacade} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdGeneratorFacade")
class IdGeneratorFacadeTest {

    private static final long MOCK_SNOWFLAKE_ID = 123456789012345L;
    private static final String MOCK_ORDER_NO = "ORD20240115123456789";
    private static final String MOCK_PAY_NO = "PAY20240115123456789";
    private static final String MOCK_RFD_NO = "RFD20240115123456789";

    @Mock
    private IdGenerator snowflakeGenerator;

    @Mock
    private OrderNoGenerator orderNoGenerator;

    private IdGeneratorFacade facade;

    @BeforeEach
    void setUp() {
        facade = new IdGeneratorFacade(snowflakeGenerator, orderNoGenerator);
    }

    @Nested
    @DisplayName("snowflakeId")
    class SnowflakeId {

        @Test
        @DisplayName("should delegate to snowflake generator and return positive id")
        void shouldDelegateToSnowflake() {
            when(snowflakeGenerator.nextId()).thenReturn(MOCK_SNOWFLAKE_ID);

            long id = facade.snowflakeId();

            assertTrue(id > 0, "Returned ID must be positive");
            assertEquals(MOCK_SNOWFLAKE_ID, id);
            verify(snowflakeGenerator).nextId();
        }
    }

    @Nested
    @DisplayName("orderNo")
    class OrderNo {

        @Test
        @DisplayName("should generate order no with ORD prefix format")
        void shouldGenerateOrderNoFormat() {
            when(orderNoGenerator.generate("ORD")).thenReturn(MOCK_ORDER_NO);

            String orderNo = facade.orderNo("ORD");

            assertTrue(orderNo.startsWith("ORD"),
                    "Order no should start with 'ORD' but was: " + orderNo);
            verify(orderNoGenerator).generate("ORD");
        }

        @Test
        @DisplayName("should delegate to orderNoGenerator when calling orderNo with no args")
        void shouldGenerateOrderNoWithoutPrefix() {
            when(orderNoGenerator.generate()).thenReturn("20240115123456789");

            String orderNo = facade.orderNo();

            assertEquals("20240115123456789", orderNo);
            verify(orderNoGenerator).generate();
        }
    }

    @Nested
    @DisplayName("payNo")
    class PayNo {

        @Test
        @DisplayName("should generate pay no with PAY prefix format")
        void shouldGeneratePayNoFormat() {
            when(orderNoGenerator.generate("PAY")).thenReturn(MOCK_PAY_NO);

            String payNo = facade.payNo();

            assertTrue(payNo.startsWith("PAY"),
                    "Pay no should start with 'PAY' but was: " + payNo);
            verify(orderNoGenerator).generate("PAY");
        }
    }

    @Nested
    @DisplayName("refundNo")
    class RefundNo {

        @Test
        @DisplayName("should generate refund no with RFD prefix format")
        void shouldGenerateRefundNoFormat() {
            when(orderNoGenerator.generate("RFD")).thenReturn(MOCK_RFD_NO);

            String refundNo = facade.refundNo();

            assertTrue(refundNo.startsWith("RFD"),
                    "Refund no should start with 'RFD' but was: " + refundNo);
            verify(orderNoGenerator).generate("RFD");
        }
    }
}
