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
        @DisplayName("should increment active connections count")
        void onConnect_shouldIncrementActiveConnections() {
            metrics.onConnect();

            assertEquals(1, metrics.getActiveConnections());
        }

        @Test
        @DisplayName("should accumulate on multiple connects")
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
        @DisplayName("should decrement active connections after connect then disconnect")
        void onDisconnect_shouldDecrementActiveConnections() {
            metrics.onConnect();
            metrics.onDisconnect();

            assertEquals(0, metrics.getActiveConnections());
        }

        @Test
        @DisplayName("should not go below zero when disconnecting without a prior connect")
        void onDisconnect_shouldNotGoBelowZero() {
            metrics.onDisconnect();

            assertEquals(0, metrics.getActiveConnections());
        }
    }

    @Nested
    @DisplayName("onMessageSent")
    class OnMessageSent {

        @Test
        @DisplayName("should increment total messages sent counter")
        void onMessageSent_shouldIncrementCounter() {
            metrics.onMessageSent();

            assertEquals(1L, metrics.getTotalMessagesSent());
        }

        @Test
        @DisplayName("should accumulate on multiple sends")
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
        @DisplayName("should register eagle.websocket.connections.active gauge in registry")
        void init_shouldRegisterGaugeMetric() {
            Optional<Meter> meter = registry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals("eagle.websocket.connections.active"))
                    .findFirst();

            assertTrue(meter.isPresent(), "Gauge 'eagle.websocket.connections.active' should be registered");
        }

        @Test
        @DisplayName("gauge value should reflect live active connections count")
        void init_gaugeShouldReflectActiveConnections() {
            Gauge gauge = registry.find("eagle.websocket.connections.active").gauge();
            assertNotNull(gauge);

            metrics.onConnect();
            metrics.onConnect();

            assertEquals(2.0, gauge.value(), 0.001);
        }
    }
}
