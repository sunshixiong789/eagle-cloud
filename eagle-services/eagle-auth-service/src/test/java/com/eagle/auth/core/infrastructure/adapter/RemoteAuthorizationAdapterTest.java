package com.eagle.auth.core.infrastructure.adapter;

import com.eagle.auth.core.domain.port.AuthorizationInfo;
import com.eagle.auth.core.infrastructure.remote.SystemAuthorizationClient;
import com.eagle.auth.core.infrastructure.remote.dto.AuthorizationInfoDto;
import com.eagle.common.exception.ErrorCode;
import com.eagle.common.exception.NotFoundException;
import tools.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteAuthorizationAdapterTest {

    private static final Long ACCOUNT_ID = 1001L;
    private static final String CACHE_KEY = "eagle:auth:authorization:" + ACCOUNT_ID;

    @Mock
    SystemAuthorizationClient client;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOps;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    RemoteAuthorizationAdapter adapter;

    @Nested
    @DisplayName("findAuthorizationInfo - 主路径")
    class HappyPath {

        @Test
        @DisplayName("远程成功时返回授权信息并写入缓存")
        void shouldReturnInfoAndCache() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            AuthorizationInfoDto dto = new AuthorizationInfoDto("张三", "https://cdn.example.com/avatar/zhangsan.jpg", Set.of("ROLE_admin"));
            when(client.findByAccountId(ACCOUNT_ID)).thenReturn(dto);

            Optional<AuthorizationInfo> result = adapter.findAuthorizationInfo(ACCOUNT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("张三");
            assertThat(result.get().roleCodes()).containsExactly("ROLE_admin");
            verify(valueOps).set(eq(CACHE_KEY), anyString(), eq(Duration.ofHours(24)));
        }

        @Test
        @DisplayName("远程返回 roleCodes=null 时应规范化为空集合")
        void shouldNormalizeNullRoles() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(client.findByAccountId(ACCOUNT_ID))
                    .thenReturn(new AuthorizationInfoDto("Bob", null, null));

            Optional<AuthorizationInfo> result = adapter.findAuthorizationInfo(ACCOUNT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().roleCodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAuthorizationInfo - 404 业务正常路径")
    class NotFoundPath {

        @Test
        @DisplayName("远程 404 → 返回 empty,不写缓存、不触发降级")
        void shouldReturnEmptyOn404() {
            when(client.findByAccountId(ACCOUNT_ID))
                    .thenThrow(new NotFoundException(stubCode(), "user not found"));

            Optional<AuthorizationInfo> result = adapter.findAuthorizationInfo(ACCOUNT_ID);

            assertThat(result).isEmpty();
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("findFromCache - 降级路径")
    class FallbackPath {

        @Test
        @DisplayName("RestClientException + 缓存命中 → 返回缓存快照")
        void shouldServeFromCacheOnRestClientException() throws Exception {
            String json = objectMapper.writeValueAsString(
                    new AuthorizationInfoDto("缓存的人", null, Set.of("ROLE_cached")));
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(CACHE_KEY)).thenReturn(json);

            Optional<AuthorizationInfo> result = adapter.findFromCache(ACCOUNT_ID,
                    new ResourceAccessException("connect refused"));

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("缓存的人");
            assertThat(result.get().roleCodes()).containsExactly("ROLE_cached");
        }

        @Test
        @DisplayName("CallNotPermittedException(熔断开路) + 缓存未命中 → 返回 empty")
        void shouldReturnEmptyOnCircuitOpenWithoutCache() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(CACHE_KEY)).thenReturn(null);

            Optional<AuthorizationInfo> result = adapter.findFromCache(ACCOUNT_ID,
                    CallNotPermittedException.createCallNotPermittedException(
                            io.github.resilience4j.circuitbreaker.CircuitBreaker
                                    .ofDefaults("eagle-default")));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("非降级类异常(NPE 等) → 直接上抛,不查缓存")
        void shouldRethrowOnNonFallbackError() {
            assertThatThrownBy(() -> adapter.findFromCache(ACCOUNT_ID,
                    new NullPointerException("programming error")))
                    .isInstanceOf(NullPointerException.class);
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("缓存读取异常时回退为 empty,不污染主流程")
        void shouldReturnEmptyOnCacheReadFailure() {
            when(redisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(CACHE_KEY)).thenThrow(new RuntimeException("redis down"));

            Optional<AuthorizationInfo> result = adapter.findFromCache(ACCOUNT_ID,
                    new ResourceAccessException("timeout"));

            assertThat(result).isEmpty();
        }
    }

    private static ErrorCode stubCode() {
        return new ErrorCode() {
            @Override
            public Meta meta() {
                return new Meta(404, "test.not.found", "not found");
            }
        };
    }
}
