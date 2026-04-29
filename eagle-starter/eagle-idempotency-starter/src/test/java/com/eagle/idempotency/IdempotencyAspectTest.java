package com.eagle.idempotency;

import com.eagle.common.exception.DomainException;
import com.eagle.idempotency.annotation.Idempotent;
import com.eagle.idempotency.annotation.IdempotencyKey;
import com.eagle.idempotency.annotation.IdempotencyMode;
import com.eagle.idempotency.aspect.IdempotencyAspect;
import com.eagle.idempotency.properties.IdempotencyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IdempotencyAspect} 单元测试。
 *
 * <p>通过直接调用 {@code around()} 方法，配合 mock 的 {@link Idempotent} 注解和
 * {@link ProceedingJoinPoint}，覆盖 TOKEN / BUSINESS_KEY / RESULT_CACHE 三种模式。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyAspect")
class IdempotencyAspectTest {

    private static final String TOKEN_HEADER = "X-Idempotency-Token";
    private static final String SAMPLE_TOKEN = "abc123token";
    private static final String KEY_PREFIX = "eagle:idempotency:";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @SuppressWarnings("rawtypes")
    @Mock
    private RBucket bucket;

    private IdempotencyAspect aspect;
    private IdempotencyProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new IdempotencyProperties();
        properties.setKeyPrefix(KEY_PREFIX);
        properties.setTokenExpireSeconds(300);
        properties.setResultCacheSeconds(86400);
        objectMapper = new ObjectMapper();
        aspect = new IdempotencyAspect(redissonClient, properties, request, objectMapper, applicationContext);
    }

    // ==================== TOKEN 模式 ====================

    @Nested
    @DisplayName("TOKEN 模式")
    class TokenMode {

        @Test
        @DisplayName("shouldProceedWhenTokenConsumedSuccessfully — bucket.delete() 返回 true 时正常放行")
        @SuppressWarnings("unchecked")
        void shouldProceedWhenTokenConsumedSuccessfully() throws Throwable {
            Idempotent annotation = buildAnnotation(IdempotencyMode.TOKEN, "", TOKEN_HEADER, "");
            String expectedResult = "ok";

            when(request.getHeader(TOKEN_HEADER)).thenReturn(SAMPLE_TOKEN);
            when(redissonClient.getBucket(KEY_PREFIX + "token:" + SAMPLE_TOKEN)).thenReturn(bucket);
            when(bucket.delete()).thenReturn(true);
            when(joinPoint.proceed()).thenReturn(expectedResult);

            Object result = aspect.around(joinPoint, annotation);

            assertEquals(expectedResult, result);
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("shouldThrowWhenTokenMissing — Header 无 Token 时抛出 DomainException")
        void shouldThrowWhenTokenMissing() {
            Idempotent annotation = buildAnnotation(IdempotencyMode.TOKEN, "", TOKEN_HEADER, "");

            when(request.getHeader(TOKEN_HEADER)).thenReturn(null);

            assertThrows(DomainException.class, () -> aspect.around(joinPoint, annotation));
        }

        @Test
        @DisplayName("shouldThrowWhenTokenAlreadyConsumed — bucket.delete() 返回 false 时抛出 DomainException")
        @SuppressWarnings("unchecked")
        void shouldThrowWhenTokenAlreadyConsumed() {
            Idempotent annotation = buildAnnotation(IdempotencyMode.TOKEN, "", TOKEN_HEADER, "");

            when(request.getHeader(TOKEN_HEADER)).thenReturn(SAMPLE_TOKEN);
            when(redissonClient.getBucket(KEY_PREFIX + "token:" + SAMPLE_TOKEN)).thenReturn(bucket);
            when(bucket.delete()).thenReturn(false);

            assertThrows(DomainException.class, () -> aspect.around(joinPoint, annotation));
        }
    }

    // ==================== BUSINESS_KEY 模式 ====================

    @Nested
    @DisplayName("BUSINESS_KEY 模式")
    class BusinessKeyMode {

        @Test
        @DisplayName("shouldProceedOnFirstRequest — setIfAbsent 返回 true 时正常放行")
        @SuppressWarnings("unchecked")
        void shouldProceedOnFirstRequest() throws Throwable {
            String spelKey = "#orderNo";
            Idempotent annotation = buildAnnotation(IdempotencyMode.BUSINESS_KEY, spelKey, TOKEN_HEADER, "");

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"orderNo"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{"ORDER-001"});
            when(redissonClient.getBucket(KEY_PREFIX + "biz:ORDER-001")).thenReturn(bucket);
            when(bucket.setIfAbsent(eq("1"), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("created");

            Object result = aspect.around(joinPoint, annotation);

            assertEquals("created", result);
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("shouldThrowOnDuplicateRequest — setIfAbsent 返回 false 时抛出 DomainException")
        @SuppressWarnings("unchecked")
        void shouldThrowOnDuplicateRequest() {
            String spelKey = "#orderNo";
            Idempotent annotation = buildAnnotation(IdempotencyMode.BUSINESS_KEY, spelKey, TOKEN_HEADER, "");

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"orderNo"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{"ORDER-001"});
            when(redissonClient.getBucket(KEY_PREFIX + "biz:ORDER-001")).thenReturn(bucket);
            when(bucket.setIfAbsent(eq("1"), anyLong(), any(TimeUnit.class))).thenReturn(false);

            assertThrows(DomainException.class, () -> aspect.around(joinPoint, annotation));
        }

        @Test
        @DisplayName("shouldThrowWhenKeyEmpty — SpEL 解析为空时抛出 DomainException")
        @SuppressWarnings("unchecked")
        void shouldThrowWhenKeyEmpty() {
            // key 为空字符串，也没有 keyExtractor，也没有 @IdempotencyKey 字段
            Idempotent annotation = buildAnnotation(IdempotencyMode.BUSINESS_KEY, "", TOKEN_HEADER, "");

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{});
            when(joinPoint.getArgs()).thenReturn(new Object[]{});

            assertThrows(DomainException.class, () -> aspect.around(joinPoint, annotation));
        }
    }

    // ==================== RESULT_CACHE 模式 ====================

    @Nested
    @DisplayName("RESULT_CACHE 模式")
    class ResultCacheMode {

        @Test
        @DisplayName("shouldReturnCachedResultOnHit — 缓存命中时直接返回，不调用 joinPoint.proceed()")
        @SuppressWarnings("unchecked")
        void shouldReturnCachedResultOnHit() throws Throwable {
            Idempotent annotation = buildAnnotation(IdempotencyMode.RESULT_CACHE, "", TOKEN_HEADER, "");
            String cachedJson = "\"hello\"";
            String resultKey = KEY_PREFIX + "result:" + SAMPLE_TOKEN;
            String typeKey = resultKey + ":type";

            RBucket<String> resultBucket = mock(RBucket.class);
            RBucket<String> typeBucket = mock(RBucket.class);

            when(request.getHeader(TOKEN_HEADER)).thenReturn(SAMPLE_TOKEN);
            when(redissonClient.getBucket(resultKey)).thenReturn(resultBucket);
            when(redissonClient.getBucket(typeKey)).thenReturn(typeBucket);
            when(resultBucket.get()).thenReturn(cachedJson);
            when(typeBucket.get()).thenReturn("java.lang.String");

            Object result = aspect.around(joinPoint, annotation);

            assertEquals("hello", result);
            verify(joinPoint, never()).proceed();
        }

        @Test
        @DisplayName("shouldProceedAndCacheOnMiss — 缓存未命中时执行并缓存结果")
        @SuppressWarnings("unchecked")
        void shouldProceedAndCacheOnMiss() throws Throwable {
            Idempotent annotation = buildAnnotation(IdempotencyMode.RESULT_CACHE, "", TOKEN_HEADER, "");
            String resultKey = KEY_PREFIX + "result:" + SAMPLE_TOKEN;
            String typeKey = resultKey + ":type";

            RBucket<String> resultBucket = mock(RBucket.class);
            RBucket<String> typeBucket = mock(RBucket.class);

            // 模拟目标方法签名以获取返回类型
            Method method = SampleService.class.getMethod("doWork");
            when(request.getHeader(TOKEN_HEADER)).thenReturn(SAMPLE_TOKEN);
            when(redissonClient.getBucket(resultKey)).thenReturn(resultBucket);
            when(redissonClient.getBucket(typeKey)).thenReturn(typeBucket);
            when(resultBucket.get()).thenReturn(null);
            when(typeBucket.get()).thenReturn(null);
            when(joinPoint.proceed()).thenReturn("computed");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(method);

            Object result = aspect.around(joinPoint, annotation);

            assertEquals("computed", result);
            verify(joinPoint).proceed();
            verify(resultBucket).set(anyString(), anyLong(), any(TimeUnit.class));
            verify(typeBucket).set(anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("shouldThrowWhenTokenMissing — 无 Token 时抛出 DomainException")
        void shouldThrowWhenTokenMissing() {
            Idempotent annotation = buildAnnotation(IdempotencyMode.RESULT_CACHE, "", TOKEN_HEADER, "");
            when(request.getHeader(TOKEN_HEADER)).thenReturn("");

            assertThrows(DomainException.class, () -> aspect.around(joinPoint, annotation));
        }

        @Test
        @DisplayName("shouldFallbackOnDeserializationFailure — 反序列化失败时降级重新执行")
        @SuppressWarnings("unchecked")
        void shouldFallbackOnDeserializationFailure() throws Throwable {
            Idempotent annotation = buildAnnotation(IdempotencyMode.RESULT_CACHE, "", TOKEN_HEADER, "");
            String resultKey = KEY_PREFIX + "result:" + SAMPLE_TOKEN;
            String typeKey = resultKey + ":type";

            RBucket<String> resultBucket = mock(RBucket.class);
            RBucket<String> typeBucket = mock(RBucket.class);

            // 返回一个不兼容的类型名（类名存在但 JSON 与类型不匹配）
            when(request.getHeader(TOKEN_HEADER)).thenReturn(SAMPLE_TOKEN);
            when(redissonClient.getBucket(resultKey)).thenReturn(resultBucket);
            when(redissonClient.getBucket(typeKey)).thenReturn(typeBucket);
            // 缓存有数据，但 JSON 内容无法反序列化为 int 数组
            when(resultBucket.get()).thenReturn("\"notAnIntArray\"");
            when(typeBucket.get()).thenReturn("[I"); // int[] 类型名
            when(joinPoint.proceed()).thenReturn("fallback-result");
            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getMethod()).thenReturn(SampleService.class.getMethod("doWork"));

            // 反序列化失败后降级执行 joinPoint.proceed()
            Object result = aspect.around(joinPoint, annotation);

            assertNotNull(result);
            verify(joinPoint).proceed();
        }
    }

    // ==================== @IdempotencyKey 字段扫描 ====================

    @Nested
    @DisplayName("@IdempotencyKey 字段扫描")
    class FieldAnnotationMode {

        @Test
        @DisplayName("shouldBuildKeyFromAnnotatedFields — 带注解字段正确拼接为业务键")
        @SuppressWarnings("unchecked")
        void shouldBuildKeyFromAnnotatedFields() throws Throwable {
            // key="" keyExtractor="" 触发字段扫描路径
            Idempotent annotation = buildAnnotation(IdempotencyMode.BUSINESS_KEY, "", TOKEN_HEADER, "");

            SampleRequest dto = new SampleRequest("ORD-999", 42L);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getParameterNames()).thenReturn(new String[]{"req"});
            when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
            when(redissonClient.getBucket(anyString())).thenReturn(bucket);
            when(bucket.setIfAbsent(eq("1"), anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(joinPoint.proceed()).thenReturn("done");

            aspect.around(joinPoint, annotation);

            // 验证 redis key 包含字段值，格式：eagle:idempotency:biz:orderNo:ORD-999|userId:42
            verify(redissonClient).getBucket(KEY_PREFIX + "biz:orderNo:ORD-999|userId:42");
        }
    }

    // ==================== 辅助类型 ====================

    /** 用于反射获取方法签名。 */
    @SuppressWarnings("unused")
    static class SampleService {
        public String doWork() {
            return "work";
        }
    }

    /** 带 @IdempotencyKey 注解的测试 DTO。 */
    static class SampleRequest {

        @IdempotencyKey
        private final String orderNo;

        @IdempotencyKey
        private final Long userId;

        SampleRequest(String orderNo, Long userId) {
            this.orderNo = orderNo;
            this.userId = userId;
        }
    }

    /**
     * 构建 {@link Idempotent} mock 注解。
     *
     * @param mode        幂等模式
     * @param key         SpEL 表达式（BUSINESS_KEY 模式）
     * @param tokenHeader Token Header 名称
     * @param extractor   keyExtractor Bean 名称
     * @return mock 后的 {@link Idempotent} 实例
     */
    private Idempotent buildAnnotation(IdempotencyMode mode, String key,
                                       String tokenHeader, String extractor) {
        Idempotent annotation = mock(Idempotent.class);
        when(annotation.mode()).thenReturn(mode);
        when(annotation.key()).thenReturn(key);
        when(annotation.tokenHeader()).thenReturn(tokenHeader);
        when(annotation.keyExtractor()).thenReturn(extractor);
        return annotation;
    }
}
