package com.eagle.rocketmq.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisIdempotencyChecker")
class RedisIdempotencyCheckerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private RedisIdempotencyChecker checker;
    private IdempotencyProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IdempotencyProperties();
        checker = new RedisIdempotencyChecker(redisTemplate, properties);
    }

    @Nested
    @DisplayName("firstTime")
    class FirstTime {

        @Test
        @DisplayName("Redis 占位成功 → true(继续处理)")
        void returnsTrueOnFirstSeen() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq("eagle:mq:idempotent:evt-1"), eq("1"), any(Duration.class)))
                    .thenReturn(Boolean.TRUE);

            assertThat(checker.firstTime("evt-1")).isTrue();
        }

        @Test
        @DisplayName("Redis 已存在 → false(跳过处理)")
        void returnsFalseOnDuplicate() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq("eagle:mq:idempotent:evt-1"), eq("1"), any(Duration.class)))
                    .thenReturn(Boolean.FALSE);

            assertThat(checker.firstTime("evt-1")).isFalse();
        }

        @Test
        @DisplayName("Redis 抛异常 → 降级返回 true(保守:重复消费胜过丢消息)")
        void degradesOnRedisFailure() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(), any(), any(Duration.class)))
                    .thenThrow(new QueryTimeoutException("redis down"));

            assertThat(checker.firstTime("evt-1")).isTrue();
        }

        @Test
        @DisplayName("eventId 为 null/空 → 直接放行 true,不调 Redis")
        void nullEventIdBypassesRedis() {
            assertThat(checker.firstTime(null)).isTrue();
            assertThat(checker.firstTime("")).isTrue();

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("自定义 TTL 透传到 setIfAbsent")
        void customTtlPropagated() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(), any(), eq(Duration.ofMinutes(5))))
                    .thenReturn(Boolean.TRUE);

            assertThat(checker.firstTime("evt-1", Duration.ofMinutes(5))).isTrue();
        }
    }

    @Nested
    @DisplayName("isDuplicate (语义糖)")
    class IsDuplicate {

        @Test
        @DisplayName("首次见到 → false")
        void firstSeenReturnsFalse() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(), any(), any(Duration.class)))
                    .thenReturn(Boolean.TRUE);

            assertThat(checker.isDuplicate("evt-1")).isFalse();
        }

        @Test
        @DisplayName("已存在 → true")
        void existingReturnsTrue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(any(), any(), any(Duration.class)))
                    .thenReturn(Boolean.FALSE);

            assertThat(checker.isDuplicate("evt-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("配置覆盖")
    class Config {

        @Test
        @DisplayName("自定义 keyPrefix 与 defaultTtl 生效")
        void customConfigApplied() {
            properties.setKeyPrefix("custom:idem:");
            properties.setDefaultTtl(Duration.ofMinutes(10));
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq("custom:idem:evt-1"), eq("1"), eq(Duration.ofMinutes(10))))
                    .thenReturn(Boolean.TRUE);

            assertThat(checker.firstTime("evt-1")).isTrue();
        }
    }
}
