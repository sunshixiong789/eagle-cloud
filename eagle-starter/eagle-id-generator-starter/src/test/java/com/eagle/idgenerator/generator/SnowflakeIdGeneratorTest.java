package com.eagle.idgenerator.generator;

import com.eagle.idgenerator.properties.IdGeneratorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SnowflakeIdGenerator} 单元测试。
 */
@DisplayName("SnowflakeIdGenerator")
class SnowflakeIdGeneratorTest {

    private static final int SINGLE_THREAD_COUNT = 1000;
    private static final int THREAD_COUNT = 10;
    private static final int IDS_PER_THREAD = 100;
    private static final long CLOCK_SKEW_MS = 5000L;

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        IdGeneratorProperties props = new IdGeneratorProperties();
        props.setWorkerId(1);
        props.setDatacenterId(1);
        props.setSequence(0);
        generator = new SnowflakeIdGenerator(props);
    }

    @Nested
    @DisplayName("nextId")
    class NextId {

        @Test
        @DisplayName("should generate unique ids when called sequentially 1000 times")
        void shouldGenerateUniqueIds() {
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < SINGLE_THREAD_COUNT; i++) {
                ids.add(generator.nextId());
            }
            assertEquals(SINGLE_THREAD_COUNT, ids.size(),
                    "All " + SINGLE_THREAD_COUNT + " sequentially generated IDs must be unique");
        }

        @Test
        @DisplayName("should generate unique ids when called concurrently from 10 threads each producing 100 ids")
        void shouldGenerateConcurrentlyUniqueIds() throws InterruptedException {
            int totalIds = THREAD_COUNT * IDS_PER_THREAD;
            CopyOnWriteArrayList<Long> ids = new CopyOnWriteArrayList<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

            for (int t = 0; t < THREAD_COUNT; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < IDS_PER_THREAD; i++) {
                            ids.add(generator.nextId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            Set<Long> uniqueIds = new HashSet<>(ids);
            assertEquals(totalIds, uniqueIds.size(),
                    "All " + totalIds + " concurrently generated IDs must be unique");
        }

        @Test
        @DisplayName("should throw IllegalStateException when clock moves backwards")
        void shouldThrowWhenClockMovesBack() throws Exception {
            // Prime the generator so lastTimestamp is set to a real value
            generator.nextId();

            // Reflect lastTimestamp to a future value (simulating clock moved back from that point)
            Field lastTimestampField = SnowflakeIdGenerator.class.getDeclaredField("lastTimestamp");
            lastTimestampField.setAccessible(true);
            long futureTimestamp = System.currentTimeMillis() + CLOCK_SKEW_MS;
            lastTimestampField.set(generator, futureTimestamp);

            assertThrows(IllegalStateException.class, generator::nextId,
                    "Should throw IllegalStateException when system clock is behind lastTimestamp");
        }

        @Test
        @DisplayName("should generate positive ids")
        void shouldGeneratePositiveIds() {
            long id = generator.nextId();
            assertTrue(id > 0, "Generated ID must be positive");
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should throw IllegalArgumentException when workerId exceeds 31")
        void shouldThrowWhenWorkerIdOutOfRange() {
            IdGeneratorProperties props = new IdGeneratorProperties();
            props.setWorkerId(32);
            props.setDatacenterId(1);

            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(props));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when datacenterId exceeds 31")
        void shouldThrowWhenDatacenterIdOutOfRange() {
            IdGeneratorProperties props = new IdGeneratorProperties();
            props.setWorkerId(1);
            props.setDatacenterId(32);

            assertThrows(IllegalArgumentException.class,
                    () -> new SnowflakeIdGenerator(props));
        }
    }
}
