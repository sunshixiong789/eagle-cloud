package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.NanoIdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import com.eagle.idgenerator.generator.TsidIdGenerator;
import com.eagle.idgenerator.generator.UuidIdGenerator;
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
    private IdGenerator defaultGenerator;
    @Mock
    private UuidIdGenerator uuidGenerator;
    @Mock
    private TsidIdGenerator tsidGenerator;
    @Mock
    private NanoIdGenerator nanoIdGenerator;
    @Mock
    private OrderNoGenerator orderNoGenerator;

    private IdGeneratorFacade facade;

    @BeforeEach
    void setUp() {
        facade = new IdGeneratorFacade(
                defaultGenerator, uuidGenerator, tsidGenerator, nanoIdGenerator, orderNoGenerator);
    }

    @Nested
    @DisplayName("snowflakeId")
    class SnowflakeId {

        @Test
        @DisplayName("应委托到默认")
        void shouldDelegateToDefault() {
            when(defaultGenerator.nextId()).thenReturn(MOCK_SNOWFLAKE_ID);

            long id = facade.snowflakeId();

            assertTrue(id > 0, "Returned ID must be positive");
            assertEquals(MOCK_SNOWFLAKE_ID, id);
            verify(defaultGenerator).nextId();
        }
    }

    @Nested
    @DisplayName("uuid")
    class Uuid {

        @Test
        @DisplayName("应委托UUIDStr")
        void shouldDelegateUuidStr() {
            when(uuidGenerator.nextIdStr()).thenReturn("018f3a1b2c9d7e4f5g6h7i8j9k0l1m2n");

            String uuid = facade.uuid();

            assertEquals(32, uuid.length());
            verify(uuidGenerator).nextIdStr();
        }
    }

    @Nested
    @DisplayName("tsid")
    class TsidId {

        @Test
        @DisplayName("应委托TSIDStr")
        void shouldDelegateTsidStr() {
            when(tsidGenerator.nextIdStr()).thenReturn("0AXFXR7X8PWGS");

            String tsid = facade.tsidStr();

            assertEquals("0AXFXR7X8PWGS", tsid);
            verify(tsidGenerator).nextIdStr();
        }
    }

    @Nested
    @DisplayName("nanoId")
    class NanoId {

        @Test
        @DisplayName("应委托默认 Nano ID")
        void shouldDelegateDefaultNanoId() {
            when(nanoIdGenerator.nextId()).thenReturn("V1StGXR8_Z5jdHi6B-myT");

            String nano = facade.nanoId();

            assertEquals(21, nano.length());
            verify(nanoIdGenerator).nextId();
        }

        @Test
        @DisplayName("应委托指定长度 Nano ID")
        void shouldDelegateSizedNanoId() {
            when(nanoIdGenerator.nextId(8)).thenReturn("Ab3xY7zQ");

            String nano = facade.nanoId(8);

            assertEquals(8, nano.length());
            verify(nanoIdGenerator).nextId(8);
        }
    }

    @Nested
    @DisplayName("orderNo")
    class OrderNo {

        @Test
        @DisplayName("应生成排序号格式")
        void shouldGenerateOrderNoFormat() {
            when(orderNoGenerator.generate("ORD")).thenReturn(MOCK_ORDER_NO);

            String orderNo = facade.orderNo("ORD");

            assertTrue(orderNo.startsWith("ORD"));
            verify(orderNoGenerator).generate("ORD");
        }

        @Test
        @DisplayName("使用out 前缀时应生成排序号")
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
        @DisplayName("应生成支付无格式")
        void shouldGeneratePayNoFormat() {
            when(orderNoGenerator.generate("PAY")).thenReturn(MOCK_PAY_NO);

            String payNo = facade.payNo();

            assertTrue(payNo.startsWith("PAY"));
            verify(orderNoGenerator).generate("PAY");
        }
    }

    @Nested
    @DisplayName("refundNo")
    class RefundNo {

        @Test
        @DisplayName("应生成退款无格式")
        void shouldGenerateRefundNoFormat() {
            when(orderNoGenerator.generate("RFD")).thenReturn(MOCK_RFD_NO);

            String refundNo = facade.refundNo();

            assertTrue(refundNo.startsWith("RFD"));
            verify(orderNoGenerator).generate("RFD");
        }
    }
}
