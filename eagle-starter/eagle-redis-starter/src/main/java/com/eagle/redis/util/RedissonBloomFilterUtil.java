package com.eagle.redis.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * Redisson 布隆过滤器工具类。
 *
 * <p>用于防止缓存穿透：在查询缓存/DB 之前，先用布隆过滤器判断数据是否可能存在，
 * 不存在则直接返回，避免大量无效查询击穿到数据库。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>用户 ID 存在性预检（防止恶意查询不存在的 ID）</li>
 *   <li>商品/订单白名单过滤</li>
 *   <li>URL 黑名单检测</li>
 * </ul>
 *
 * <p><b>注意：</b>布隆过滤器存在一定的误判率（判断"存在"但实际不存在），
 * 但不会漏判（判断"不存在"时一定不存在）。请根据业务容忍度设置误判率。
 *
 * @author 孙士雄
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonBloomFilterUtil {

    private final RedissonClient redissonClient;

    /**
     * 初始化布隆过滤器（仅首次调用生效，已存在则跳过）。
     *
     * <pre>
     * // 示例：预期存储 100 万个用户 ID，误判率 0.1%
     * bloomFilter.init("user:exist", 1_000_000, 0.001);
     * </pre>
     *
     * @param filterName        过滤器名称
     * @param expectedInsertions 预期插入元素数量
     * @param falseProbability  期望误判率（0 ~ 1，如 0.01 表示 1%）
     * @return {@code true} 初始化成功；{@code false} 已存在，跳过初始化
     */
    public <T> boolean init(String filterName, long expectedInsertions, double falseProbability) {
        RBloomFilter<T> bloomFilter = redissonClient.getBloomFilter(filterName);
        boolean initialized = bloomFilter.tryInit(expectedInsertions, falseProbability);
        if (initialized) {
            log.info("BloomFilter [{}] initialized: expectedInsertions={}, falseProbability={}",
                    filterName, expectedInsertions, falseProbability);
        }
        return initialized;
    }

    /**
     * 向过滤器添加元素。
     *
     * @param filterName 过滤器名称
     * @param value      元素值
     * @param <T>        元素类型
     * @return {@code true} 元素是新增的；{@code false} 元素已存在
     */
    public <T> boolean add(String filterName, T value) {
        return redissonClient.<T>getBloomFilter(filterName).add(value);
    }

    /**
     * 判断元素是否可能存在于过滤器中。
     *
     * <p>返回 {@code false} 表示该元素<b>一定不存在</b>，可直接拦截；
     * 返回 {@code true} 表示该元素<b>可能存在</b>（有误判率），需进一步查询缓存/DB 确认。
     *
     * <pre>
     * // 缓存穿透防护示例
     * if (!bloomFilter.contains("user:exist", userId)) {
     *     return null; // 一定不存在，直接返回
     * }
     * return userCache.get(userId); // 可能存在，再查缓存
     * </pre>
     *
     * @param filterName 过滤器名称
     * @param value      元素值
     * @param <T>        元素类型
     * @return {@code true} 可能存在；{@code false} 一定不存在
     */
    public <T> boolean contains(String filterName, T value) {
        return redissonClient.<T>getBloomFilter(filterName).contains(value);
    }

    /**
     * 批量添加元素（启动时预热，从 DB 加载所有有效 ID）。
     *
     * <pre>
     * // 启动预热示例
     * List&lt;Long&gt; allUserIds = userRepository.findAllIds();
     * bloomFilter.addAll("user:exist", allUserIds);
     * </pre>
     *
     * @param filterName 过滤器名称
     * @param values     元素集合
     * @param <T>        元素类型
     */
    public <T> void addAll(String filterName, Iterable<T> values) {
        RBloomFilter<T> bloomFilter = redissonClient.getBloomFilter(filterName);
        for (T value : values) {
            bloomFilter.add(value);
        }
    }

    /**
     * 查询过滤器中已添加的元素数量。
     *
     * @param filterName 过滤器名称
     * @return 元素数量
     */
    public long count(String filterName) {
        return redissonClient.getBloomFilter(filterName).count();
    }

    /**
     * 删除布隆过滤器（不可逆，需重新初始化和预热）。
     *
     * @param filterName 过滤器名称
     */
    public void delete(String filterName) {
        redissonClient.getBloomFilter(filterName).delete();
        log.warn("BloomFilter [{}] deleted", filterName);
    }
}
