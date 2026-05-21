package com.eagle.idempotency.aspect;

import com.eagle.idempotency.annotation.IdempotencyKey;
import com.eagle.idempotency.annotation.IdempotencyMode;
import com.eagle.idempotency.annotation.Idempotent;
import com.eagle.idempotency.exception.IdempotencyErrorCode;
import com.eagle.idempotency.extractor.IdempotencyKeyExtractor;
import com.eagle.idempotency.properties.IdempotencyProperties;
import com.eagle.idempotency.support.IdempotencyTokenResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 幂等性 AOP 切面。
 *
 * <p>拦截所有标注了 {@link Idempotent} 的方法，根据模式执行幂等校验：
 * <ul>
 *   <li>{@link IdempotencyMode#TOKEN}：从请求 Header 取 Token，原子性判断是否已被使用</li>
 *   <li>{@link IdempotencyMode#BUSINESS_KEY}：通过 SpEL / {@link IdempotencyKeyExtractor} /
 *       {@link IdempotencyKey} 字段注解解析业务键，Redis setNX 防重</li>
 *   <li>{@link IdempotencyMode#RESULT_CACHE}：首次执行成功后缓存响应结果，
 *       重复请求直接返回缓存结果，不报错，对幂等重试友好</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Aspect
public class IdempotencyAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    /**
     * Redis key 中缓存的结果类型后缀 key 部分
     */
    private static final String RESULT_TYPE_SUFFIX = ":type";
    private final RedissonClient redissonClient;
    private final IdempotencyProperties properties;
    private final IdempotencyTokenResolver tokenResolver;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    /**
     * 构造幂等性切面。
     *
     * @param redissonClient     Redisson 客户端
     * @param properties         幂等性配置属性
     * @param tokenResolver      当前 HTTP 请求 Token 解析器
     * @param objectMapper       Jackson ObjectMapper，用于 RESULT_CACHE 模式序列化/反序列化响应
     * @param applicationContext Spring 容器，用于按名称获取 {@link IdempotencyKeyExtractor} Bean
     */
    public IdempotencyAspect(
            RedissonClient redissonClient,
            IdempotencyProperties properties,
            IdempotencyTokenResolver tokenResolver,
            ObjectMapper objectMapper,
            ApplicationContext applicationContext) {
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.tokenResolver = tokenResolver;
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }

    /**
     * 环绕通知：拦截 {@link Idempotent} 注解标注的方法执行幂等校验。
     *
     * @param joinPoint  切点信息
     * @param idempotent 方法上的幂等注解
     * @return 目标方法返回值（RESULT_CACHE 模式下可能为缓存的反序列化对象）
     * @throws Throwable 目标方法抛出的异常或幂等校验失败异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        return switch (idempotent.mode()) {
            case TOKEN -> handleTokenModeAndProceed(joinPoint, idempotent);
            case RESULT_CACHE -> handleResultCacheMode(joinPoint, idempotent);
            default -> {
                handleBusinessKeyMode(joinPoint, idempotent);
                yield joinPoint.proceed();
            }
        };
    }

    /**
     * TOKEN 模式处理：从请求 Header 取 Token，原子性删除判断是否有效，然后执行目标方法。
     *
     * @param joinPoint  切点信息
     * @param idempotent 幂等注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常或 Token 校验失败异常
     */
    private Object handleTokenModeAndProceed(ProceedingJoinPoint joinPoint, Idempotent idempotent)
            throws Throwable {
        String token = tokenResolver.resolveToken(idempotent.tokenHeader());
        if (!StringUtils.hasText(token)) {
            log.warn("Idempotency token missing, header: {}", idempotent.tokenHeader());
            throw IdempotencyErrorCode.IDEMPOTENCY_TOKEN_MISSING.toDomainException();
        }

        String redisKey = properties.getKeyPrefix() + "token:" + token;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);

        // 原子性删除：成功则 token 有效且首次使用；失败则 token 不存在或已使用
        boolean consumed = bucket.delete();
        if (!consumed) {
            log.warn("Idempotency token invalid or already consumed: {}", token);
            throw IdempotencyErrorCode.IDEMPOTENCY_TOKEN_INVALID.toDomainException();
        }

        log.debug("Idempotency token consumed: {}", token);
        return joinPoint.proceed();
    }

    /**
     * RESULT_CACHE 模式处理：先查缓存，有则直接返回；无则执行并将结果存入缓存。
     *
     * <p>缓存结构（以 token 为 key）：
     * <ul>
     *   <li>{@code keyPrefix + "result:" + token} — 序列化后的 JSON 结果</li>
     *   <li>{@code keyPrefix + "result:" + token + ":type"} — 返回值的完整类名（反序列化时使用）</li>
     * </ul>
     *
     * @param joinPoint  切点信息
     * @param idempotent 幂等注解
     * @return 目标方法返回值或缓存命中时的历史结果
     * @throws Throwable 目标方法抛出的异常
     */
    private Object handleResultCacheMode(ProceedingJoinPoint joinPoint, Idempotent idempotent)
            throws Throwable {
        String token = tokenResolver.resolveToken(idempotent.tokenHeader());
        if (!StringUtils.hasText(token)) {
            log.warn("RESULT_CACHE: idempotency token missing, header: {}", idempotent.tokenHeader());
            throw IdempotencyErrorCode.IDEMPOTENCY_TOKEN_MISSING.toDomainException();
        }

        String resultKey = properties.getKeyPrefix() + "result:" + token;
        String typeKey = resultKey + RESULT_TYPE_SUFFIX;

        RBucket<String> resultBucket = redissonClient.getBucket(resultKey);
        RBucket<String> typeBucket = redissonClient.getBucket(typeKey);

        String cachedJson = resultBucket.get();
        String cachedTypeName = typeBucket.get();

        // 缓存命中：直接反序列化并返回，不执行目标方法
        if (StringUtils.hasText(cachedJson) && StringUtils.hasText(cachedTypeName)) {
            log.debug("RESULT_CACHE hit for token: {}, type: {}", token, cachedTypeName);
            try {
                Class<?> returnType = Class.forName(cachedTypeName);
                return objectMapper.readValue(cachedJson, returnType);
            } catch (Exception ex) {
                // 反序列化失败（如类型不兼容），降级为重新执行目标方法
                log.warn("RESULT_CACHE deserialization failed for token: {}, fallback to proceed. cause: {}",
                        token, ex.getMessage());
            }
        }

        // 缓存未命中：执行目标方法，成功后缓存结果
        Object result = joinPoint.proceed();

        if (result != null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String returnTypeName = method.getReturnType().getName();
            try {
                String json = objectMapper.writeValueAsString(result);
                long ttl = properties.getResultCacheSeconds();
                resultBucket.set(json, Duration.ofSeconds(ttl));
                typeBucket.set(returnTypeName, Duration.ofSeconds(ttl));
                log.debug("RESULT_CACHE stored for token: {}, type: {}, ttl: {}s", token, returnTypeName, ttl);
            } catch (Exception ex) {
                // 序列化失败不影响主流程，只记录警告
                log.warn("RESULT_CACHE serialization failed for token: {}, type: {}. cause: {}",
                        token, returnTypeName, ex.getMessage());
            }
        }

        return result;
    }

    /**
     * BUSINESS_KEY 模式处理：按优先级解析业务键（keyExtractor > SpEL > @IdempotencyKey 字段），
     * Redis setNX 防重。
     *
     * <p>业务键解析优先级：
     * <ol>
     *   <li>若 {@link Idempotent#keyExtractor()} 非空，从 Spring 容器获取对应 Bean 提取键</li>
     *   <li>若 {@link Idempotent#key()} 非空，通过 SpEL 表达式解析键</li>
     *   <li>自动扫描切点第一个参数对象中标有 {@link IdempotencyKey} 的字段，拼接键</li>
     * </ol>
     *
     * @param joinPoint  切点信息
     * @param idempotent 幂等注解
     */
    private void handleBusinessKeyMode(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String businessKey = resolveBusinessKey(joinPoint, idempotent);

        if (!StringUtils.hasText(businessKey)) {
            log.error("BUSINESS_KEY mode: resolved business key is empty. "
                    + "Please provide key(), keyExtractor(), or annotate fields with @IdempotencyKey");
            throw IdempotencyErrorCode.IDEMPOTENCY_DUPLICATE_REQUEST.toDomainException();
        }

        String redisKey = properties.getKeyPrefix() + "biz:" + businessKey;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);

        // setIfAbsent 对应 Redis SETNX：首次请求成功，重复请求返回 false
        boolean isFirst = bucket.setIfAbsent("1", Duration.ofSeconds(properties.getResultCacheSeconds()));
        if (!isFirst) {
            log.warn("Duplicate request detected for business key: {}", businessKey);
            throw IdempotencyErrorCode.IDEMPOTENCY_DUPLICATE_REQUEST.toDomainException();
        }

        log.debug("Business key idempotency check passed: {}", businessKey);
    }

    /**
     * 按优先级解析业务键。
     *
     * @param joinPoint  切点信息
     * @param idempotent 幂等注解
     * @return 业务键字符串，可能为空
     */
    private String resolveBusinessKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 1. 优先使用自定义键提取器 Bean
        String extractorBeanName = idempotent.keyExtractor();
        if (StringUtils.hasText(extractorBeanName)) {
            return resolveKeyByExtractor(joinPoint, extractorBeanName);
        }

        // 2. 使用 SpEL 表达式
        String keyExpression = idempotent.key();
        if (StringUtils.hasText(keyExpression)) {
            return resolveKeyBySpel(joinPoint, keyExpression);
        }

        // 3. 自动扫描第一个参数对象中的 @IdempotencyKey 字段
        return resolveKeyByFieldAnnotation(joinPoint);
    }

    /**
     * 通过 Spring 容器中的 {@link IdempotencyKeyExtractor} Bean 提取业务键。
     *
     * @param joinPoint         切点信息
     * @param extractorBeanName Bean 名称
     * @return 提取的业务键
     */
    private String resolveKeyByExtractor(ProceedingJoinPoint joinPoint, String extractorBeanName) {
        IdempotencyKeyExtractor extractor = applicationContext.getBean(
                extractorBeanName, IdempotencyKeyExtractor.class);
        String key = extractor.extract(joinPoint);
        if (!StringUtils.hasText(key)) {
            log.warn("IdempotencyKeyExtractor '{}' returned empty key", extractorBeanName);
        }
        return key;
    }

    /**
     * 通过 SpEL 从方法参数中解析业务键。
     *
     * @param joinPoint     切点信息
     * @param keyExpression SpEL 表达式，如 {@code "#request.orderNo"}
     * @return 解析后的业务键字符串
     */
    private String resolveKeyBySpel(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        // 同时支持 #p0、#p1 等位置参数引用
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
        }

        Object result = PARSER.parseExpression(keyExpression).getValue(context);
        return result != null ? result.toString() : "";
    }

    /**
     * 自动扫描切点第一个参数对象中标有 {@link IdempotencyKey} 注解的字段，拼接为业务键。
     *
     * <p>多字段时拼接格式为 {@code "{prefix}:{value}|{prefix}:{value}"}，
     * 字段顺序按类中声明顺序排列，{@code prefix} 优先取注解上的 {@link IdempotencyKey#prefix()}，
     * 若为空则使用字段名。
     *
     * @param joinPoint 切点信息
     * @return 拼接后的业务键，无匹配字段时返回空字符串
     */
    private String resolveKeyByFieldAnnotation(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0 || args[0] == null) {
            return "";
        }

        Object firstArg = args[0];
        Class<?> argClass = firstArg.getClass();
        List<String> keyParts = new ArrayList<>();

        // 包含父类字段（向上遍历类层次）
        Class<?> current = argClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                IdempotencyKey keyAnnotation = field.getAnnotation(IdempotencyKey.class);
                if (keyAnnotation == null) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(firstArg);
                    String prefix = StringUtils.hasText(keyAnnotation.prefix())
                            ? keyAnnotation.prefix()
                            : field.getName();
                    keyParts.add(prefix + ":" + value);
                } catch (IllegalAccessException ex) {
                    log.warn("Cannot access field '{}' on '{}' for idempotency key extraction",
                            field.getName(), argClass.getSimpleName());
                }
            }
            current = current.getSuperclass();
        }

        if (keyParts.isEmpty()) {
            log.debug("No @IdempotencyKey fields found on {}", argClass.getSimpleName());
            return "";
        }

        return String.join("|", keyParts);
    }
}
