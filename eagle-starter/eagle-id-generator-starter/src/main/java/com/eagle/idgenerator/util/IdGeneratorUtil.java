package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.NanoIdGenerator;
import com.eagle.idgenerator.generator.TsidIdGenerator;
import com.eagle.idgenerator.generator.UuidIdGenerator;
import com.github.f4b6a3.tsid.Tsid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.UUID;

/**
 * 分布式 ID 静态工具类。
 *
 * <p>持有 Spring 容器中的各 ID 生成器实例，暴露静态方法供非 Spring 管理的类
 * （如 JPA 实体、工具类）直接调用，无需注入。
 *
 * <p>使用示例：
 * <pre>{@code
 * long id = IdGeneratorUtil.nextId();        // 默认实现
 * String uuid = IdGeneratorUtil.uuid();      // UUID v7
 * String tsid = IdGeneratorUtil.tsidStr();   // TSID
 * String code = IdGeneratorUtil.nanoId(8);   // 短码
 * }</pre>
 *
 * @author sunshixiong
 */
@Slf4j
public class IdGeneratorUtil implements InitializingBean {

    private static IdGenerator defaultInstance;
    private static UuidIdGenerator uuidInstance;
    private static TsidIdGenerator tsidInstance;
    private static NanoIdGenerator nanoIdInstance;

    private final IdGenerator idGenerator;
    private final UuidIdGenerator uuidIdGenerator;
    private final TsidIdGenerator tsidIdGenerator;
    private final NanoIdGenerator nanoIdGenerator;

    public IdGeneratorUtil(
            IdGenerator idGenerator,
            UuidIdGenerator uuidIdGenerator,
            TsidIdGenerator tsidIdGenerator,
            NanoIdGenerator nanoIdGenerator) {
        this.idGenerator = idGenerator;
        this.uuidIdGenerator = uuidIdGenerator;
        this.tsidIdGenerator = tsidIdGenerator;
        this.nanoIdGenerator = nanoIdGenerator;
    }

    @Override
    public void afterPropertiesSet() {
        defaultInstance = this.idGenerator;
        uuidInstance = this.uuidIdGenerator;
        tsidInstance = this.tsidIdGenerator;
        nanoIdInstance = this.nanoIdGenerator;
        log.info("IdGeneratorUtil initialized with default={}", idGenerator.getClass().getSimpleName());
    }

    // ==================== 默认实现 ====================

    public static long nextId() {
        return require(defaultInstance).nextId();
    }

    public static String nextIdStr() {
        return require(defaultInstance).nextIdStr();
    }

    // ==================== UUID v7 ====================

    /** 32 位 UUID v7 字符串（无连字符）。 */
    public static String uuid() {
        return require(uuidInstance).nextIdStr();
    }

    /** 原始 UUID v7 对象（36 位标准格式）。 */
    public static UUID uuidV7() {
        return require(uuidInstance).nextUuid();
    }

    // ==================== TSID ====================

    public static long tsidLong() {
        return require(tsidInstance).nextId();
    }

    /** 13 位 TSID 字符串。 */
    public static String tsidStr() {
        return require(tsidInstance).nextIdStr();
    }

    public static Tsid tsid() {
        return require(tsidInstance).nextTsid();
    }

    // ==================== NanoId ====================

    public static String nanoId() {
        return require(nanoIdInstance).nextId();
    }

    public static String nanoId(int size) {
        return require(nanoIdInstance).nextId(size);
    }

    private static <T> T require(T instance) {
        if (instance == null) {
            throw new IllegalStateException(
                    "IdGeneratorUtil is not initialized. Ensure eagle-id-generator-starter is on the classpath "
                            + "and Spring context has started.");
        }
        return instance;
    }
}
