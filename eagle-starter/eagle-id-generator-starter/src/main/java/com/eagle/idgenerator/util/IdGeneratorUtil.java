package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

/**
 * 分布式 ID 静态工具类。
 *
 * <p>持有 Spring 容器中的 {@link IdGenerator} 实例，暴露静态方法供非 Spring 管理的类
 * （如 JPA 实体、工具类）直接调用，无需注入。
 *
 * <p>使用示例：
 * <pre>{@code
 * long id = IdGeneratorUtil.nextId();
 * String idStr = IdGeneratorUtil.nextIdStr();
 * }</pre>
 *
 * @author sunshixiong
 */
@Slf4j
public class IdGeneratorUtil implements InitializingBean {

    /** Spring 容器中的 IdGenerator 实例，由构造器注入后暴露给静态方法 */
    private static IdGenerator instance;

    private final IdGenerator idGenerator;

    /**
     * 构造器注入 {@link IdGenerator}，由 Spring 容器调用。
     *
     * @param idGenerator 分布式 ID 生成器实现
     */
    public IdGeneratorUtil(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public void afterPropertiesSet() {
        // Bean 初始化完成后，将实例发布到静态字段供静态方法使用
        instance = this.idGenerator;
        log.info("IdGeneratorUtil initialized with: {}", idGenerator.getClass().getSimpleName());
    }

    /**
     * 生成下一个 long 型唯一 ID。
     *
     * @return 全局唯一的 long 型 ID
     * @throws IllegalStateException 若 Spring 容器未完成初始化时调用
     */
    public static long nextId() {
        assertInitialized();
        return instance.nextId();
    }

    /**
     * 生成下一个 String 型唯一 ID。
     *
     * @return 全局唯一的字符串 ID
     * @throws IllegalStateException 若 Spring 容器未完成初始化时调用
     */
    public static String nextIdStr() {
        assertInitialized();
        return instance.nextIdStr();
    }

    /**
     * 校验工具类已完成初始化，避免在 Spring 容器启动前误调用。
     */
    private static void assertInitialized() {
        if (instance == null) {
            throw new IllegalStateException(
                    "IdGeneratorUtil is not initialized. Ensure eagle-id-generator-starter is on the classpath "
                            + "and Spring context has started.");
        }
    }
}
