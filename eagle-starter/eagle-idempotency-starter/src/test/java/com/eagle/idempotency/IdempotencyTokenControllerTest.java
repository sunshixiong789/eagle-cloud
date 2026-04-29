package com.eagle.idempotency;

import com.eagle.idempotency.controller.IdempotencyTokenController;
import com.eagle.idempotency.properties.IdempotencyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IdempotencyTokenController} 单元测试。
 *
 * <p>直接实例化 Controller，mock Redisson 依赖，
 * 验证 Token 生成逻辑和 Redis 写入行为。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyTokenController")
class IdempotencyTokenControllerTest {

    private static final String KEY_PREFIX = "eagle:idempotency:";
    private static final long TOKEN_EXPIRE_SECONDS = 300L;

    @Mock
    private RedissonClient redissonClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private RBucket bucket;

    private IdempotencyTokenController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        IdempotencyProperties properties = new IdempotencyProperties();
        properties.setKeyPrefix(KEY_PREFIX);
        properties.setTokenExpireSeconds(TOKEN_EXPIRE_SECONDS);

        when(redissonClient.getBucket(anyString())).thenReturn(bucket);

        controller = new IdempotencyTokenController(redissonClient, properties);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("shouldGenerateToken — 返回非空 Token 并将其写入 Redis")
        @SuppressWarnings("unchecked")
        void shouldGenerateToken() {
            String token = controller.generateToken();

            assertNotNull(token, "Token 不应为 null");
            assertTrue(!token.isBlank(), "Token 不应为空字符串");
            // 验证 Redis bucket 以正确的 key 前缀写入
            verify(redissonClient).getBucket(anyString());
            verify(bucket).set(eq("1"), eq(TOKEN_EXPIRE_SECONDS), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("shouldReturnDifferentTokensOnMultipleCalls — 两次调用应返回不同的 Token")
        void shouldReturnDifferentTokensOnMultipleCalls() {
            @SuppressWarnings("unchecked")
            RBucket<String> bucket2 = mock(RBucket.class);
            // 两次调用均返回独立 bucket（getBucket 参数不同）
            when(redissonClient.getBucket(anyString()))
                    .thenReturn(bucket)
                    .thenReturn(bucket2);

            String token1 = controller.generateToken();
            String token2 = controller.generateToken();

            assertNotNull(token1);
            assertNotNull(token2);
            assertNotEquals(token1, token2, "连续两次生成的 Token 应不同");
        }

        @Test
        @DisplayName("shouldUseCorrectRedisKeyPrefix — Redis key 应以配置的前缀开头")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldUseCorrectRedisKeyPrefix() {
            // 捕获实际写入的 Redis key
            final String[] capturedKey = {null};
            when(redissonClient.getBucket(anyString())).thenAnswer(inv -> {
                capturedKey[0] = inv.getArgument(0);
                return bucket;
            });

            controller.generateToken();

            assertNotNull(capturedKey[0], "Redis key 不应为 null");
            assertTrue(capturedKey[0].startsWith(KEY_PREFIX + "token:"),
                    "Redis key 应以 '" + KEY_PREFIX + "token:' 开头，实际: " + capturedKey[0]);
        }
    }
}
