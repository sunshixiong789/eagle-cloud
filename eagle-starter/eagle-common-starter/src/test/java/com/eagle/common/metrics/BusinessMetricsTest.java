package com.eagle.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BusinessMetrics}.
 *
 * <p>Uses {@link SimpleMeterRegistry} — no mocking required.
 */
class BusinessMetricsTest {

    private SimpleMeterRegistry registry;
    private BusinessMetrics businessMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        businessMetrics = new BusinessMetrics(registry);
    }

    @Nested
    @DisplayName("incrementOrderCreated")
    class IncrementOrderCreated {

        @Test
        @DisplayName("should register counter and increment to 1 on first call")
        void incrementOrderCreated_shouldRegisterCounter() {
            businessMetrics.incrementOrderCreated("app");

            Counter counter = registry.find("eagle.order.created")
                    .tag("channel", "app")
                    .counter();

            assertNotNull(counter, "Counter 'eagle.order.created' should be registered");
            assertEquals(1.0, counter.count(), 0.001);
        }

        @Test
        @DisplayName("should accumulate count on multiple calls with same channel")
        void incrementOrderCreated_shouldAccumulateCount() {
            businessMetrics.incrementOrderCreated("web");
            businessMetrics.incrementOrderCreated("web");
            businessMetrics.incrementOrderCreated("web");

            Counter counter = registry.find("eagle.order.created")
                    .tag("channel", "web")
                    .counter();

            assertNotNull(counter);
            assertEquals(3.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("incrementPaymentSuccess")
    class IncrementPaymentSuccess {

        @Test
        @DisplayName("should register counter with payment method tag")
        void incrementPaymentSuccess_shouldTagWithMethod() {
            businessMetrics.incrementPaymentSuccess("alipay");

            Counter counter = registry.find("eagle.payment.success")
                    .tag("method", "alipay")
                    .counter();

            assertNotNull(counter, "Counter 'eagle.payment.success' with tag method=alipay should exist");
            assertEquals(1.0, counter.count(), 0.001);
        }

        @Test
        @DisplayName("should track different payment methods independently")
        void incrementPaymentSuccess_shouldTrackMethodsIndependently() {
            businessMetrics.incrementPaymentSuccess("alipay");
            businessMetrics.incrementPaymentSuccess("wechat");
            businessMetrics.incrementPaymentSuccess("wechat");

            Counter alipay = registry.find("eagle.payment.success").tag("method", "alipay").counter();
            Counter wechat = registry.find("eagle.payment.success").tag("method", "wechat").counter();

            assertNotNull(alipay);
            assertNotNull(wechat);
            assertEquals(1.0, alipay.count(), 0.001);
            assertEquals(2.0, wechat.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("incrementPaymentFailed")
    class IncrementPaymentFailed {

        @Test
        @DisplayName("should register counter with reason tag")
        void incrementPaymentFailed_shouldTagWithReason() {
            businessMetrics.incrementPaymentFailed("balance_insufficient");

            Counter counter = registry.find("eagle.payment.failed")
                    .tag("reason", "balance_insufficient")
                    .counter();

            assertNotNull(counter);
            assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("startTimer and recordDuration")
    class TimerOperations {

        @Test
        @DisplayName("startTimer should return a non-null Timer.Sample")
        void startTimer_shouldReturnSample() {
            Timer.Sample sample = businessMetrics.startTimer();

            assertNotNull(sample);
        }

        @Test
        @DisplayName("recordDuration with sample should register timer in registry")
        void recordDuration_shouldRegisterTimer() {
            Timer.Sample sample = businessMetrics.startTimer();

            businessMetrics.recordDuration("inventory.deduct", sample);

            Optional<Meter> timer = registry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals("eagle.inventory.deduct"))
                    .findFirst();

            assertTrue(timer.isPresent(), "Timer 'eagle.inventory.deduct' should be registered");
        }

        @Test
        @DisplayName("recordDuration with milliseconds should register timer in registry")
        void recordDuration_withMillis_shouldRegisterTimer() {
            businessMetrics.recordDuration("order.create", 150L);

            Optional<Meter> timer = registry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals("eagle.order.create"))
                    .findFirst();

            assertTrue(timer.isPresent(), "Timer 'eagle.order.create' should be registered");
        }
    }

    @Nested
    @DisplayName("incrementOrderCancelled")
    class IncrementOrderCancelled {

        @Test
        @DisplayName("should register counter with reason tag")
        void incrementOrderCancelled_shouldTagWithReason() {
            businessMetrics.incrementOrderCancelled("timeout");

            Counter counter = registry.find("eagle.order.cancelled")
                    .tag("reason", "timeout")
                    .counter();

            assertNotNull(counter);
            assertEquals(1.0, counter.count(), 0.001);
        }
    }

    @Nested
    @DisplayName("incrementInventoryDeducted")
    class IncrementInventoryDeducted {

        @Test
        @DisplayName("should register counter with warehouse tag")
        void incrementInventoryDeducted_shouldTagWithWarehouse() {
            businessMetrics.incrementInventoryDeducted("WH-01");

            Counter counter = registry.find("eagle.inventory.deducted")
                    .tag("warehouse", "WH-01")
                    .counter();

            assertNotNull(counter);
            assertEquals(1.0, counter.count(), 0.001);
        }
    }
}
