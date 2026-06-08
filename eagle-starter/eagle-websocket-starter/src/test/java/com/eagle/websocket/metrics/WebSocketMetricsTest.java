package com.eagle.websocket.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
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
 * Unit tests for {@link WebSocketMetrics}.
 *
 * <p>Uses {@link SimpleMeterRegistry} — no mocking required.
 */
class WebSocketMetricsTest {

    private SimpleMeterRegistry registry;
    private WebSocketMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new WebSocketMetrics(registry);
        // Simulate @PostConstruct
        metrics.init();
    }

    @Nested
    @DisplayName("onConnect")
    class OnConnect {

        @Test
        @DisplayName("在连接：应递增Active连接")
        void onConnect_shouldIncrementActiveConnections() {
            metrics.onConnect();

            assertEquals(1, metrics.getActiveConnections());
        }

        @Test
        @DisplayName("在连接：多个Connects时应累加")
        void onConnect_shouldAccumulateOnMultipleConnects() {
            metrics.onConnect();
            metrics.onConnect();
            metrics.onConnect();

            assertEquals(3, metrics.getActiveConnections());
        }
    }

    @Nested
    @DisplayName("onDisconnect")
    class OnDisconnect {

        @Test
        @DisplayName("在断开连接：应DecrementActive连接")
        void onDisconnect_shouldDecrementActiveConnections() {
            metrics.onConnect();
            metrics.onDisconnect();

            assertEquals(0, metrics.getActiveConnections());
        }

        @Test
        @DisplayName("在断开连接：应不GoBelow零")
        void onDisconnect_shouldNotGoBelowZero() {
            metrics.onDisconnect();

            assertEquals(0, metrics.getActiveConnections());
        }
    }

    @Nested
    @DisplayName("onMessageSent")
    class OnMessageSent {

        @Test
        @DisplayName("在消息Sent：应递增计数器")
        void onMessageSent_shouldIncrementCounter() {
            metrics.onMessageSent();

            assertEquals(1L, metrics.getTotalMessagesSent());
        }

        @Test
        @DisplayName("在消息Sent：多个Sends时应累加")
        void onMessageSent_shouldAccumulateOnMultipleSends() {
            metrics.onMessageSent();
            metrics.onMessageSent();
            metrics.onMessageSent();

            assertEquals(3L, metrics.getTotalMessagesSent());
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("初始化：应注册GaugeMetric")
        void init_shouldRegisterGaugeMetric() {
            Optional<Meter> meter = registry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals("eagle.websocket.connections.active"))
                    .findFirst();

            assertTrue(meter.isPresent(), "Gauge 'eagle.websocket.connections.active' should be registered");
        }

        @Test
        @DisplayName("初始化：gauge应ReflectActive连接")
        void init_gaugeShouldReflectActiveConnections() {
            Gauge gauge = registry.find("eagle.websocket.connections.active").gauge();
            assertNotNull(gauge);

            metrics.onConnect();
            metrics.onConnect();

            assertEquals(2.0, gauge.value(), 0.001);
        }
    }
}
