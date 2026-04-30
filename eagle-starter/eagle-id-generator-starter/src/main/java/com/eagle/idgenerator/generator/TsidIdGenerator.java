package com.eagle.idgenerator.generator;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 TSID（Time-Sorted Unique Identifier）的分布式 ID 生成器。
 *
 * <p>TSID 是雪花算法的现代替代方案，结构（64 bit）：
 * <pre>
 * | 时间戳(42) | 节点ID(可配置) | 序列号(剩余) |
 * </pre>
 *
 * <p>相对雪花算法的优势：
 * <ul>
 *   <li>节点位 / 序列号位可动态配置（256 / 1024 / 4096 节点）</li>
 *   <li>字符串形式为 13 位 Crockford Base32（{@code "0AXFXR7X8PWGS"}），URL 安全且短于 Long</li>
 *   <li>同一作者维护 uuid-creator 与 tsid-creator，质量与可靠性已被广泛验证</li>
 *   <li>支持 long ↔ String 互转（{@link Tsid#format(String)} (String)} / {@link Tsid#toLong()}）</li>
 * </ul>
 *
 * <p>节点 ID 通过 {@code TSID_NODE} 环境变量、{@code tsidcreator.node} 系统属性，
 * 或本类构造时传入的 {@code nodeId} 参数（优先）指定。
 *
 * <p>线程安全：{@link TsidFactory} 内部线程安全。
 *
 * @author sunshixiong
 */
@Slf4j
public class TsidIdGenerator implements IdGenerator {

    private final TsidFactory tsidFactory;

    /**
     * 使用默认节点容量（1024 节点）构造 TSID 生成器。
     *
     * @param nodeId 节点 ID，范围 [0, 1023]，集群部署时各实例须配置不同值
     */
    public TsidIdGenerator(int nodeId) {
        this.tsidFactory = TsidFactory.newInstance1024(nodeId);
        log.info("TsidIdGenerator initialized with 1024-node capacity: nodeId={}", nodeId);
    }

    /**
     * 使用自定义节点容量构造 TSID 生成器。
     *
     * @param nodeId   节点 ID
     * @param nodeBits 节点位数（8=256 节点 / 10=1024 节点 / 12=4096 节点）
     */
    public TsidIdGenerator(int nodeId, int nodeBits) {
        this.tsidFactory = TsidFactory.builder()
                .withNode(nodeId)
                .withNodeBits(nodeBits)
                .build();
        log.info("TsidIdGenerator initialized: nodeId={}, nodeBits={}", nodeId, nodeBits);
    }

    /**
     * 生成下一个 TSID 的 long 形式。
     *
     * @return 64 位 long 型 TSID（按生成时间趋势递增）
     */
    @Override
    public long nextId() {
        return tsidFactory.create().toLong();
    }

    /**
     * 生成下一个 TSID 的 13 位 Crockford Base32 字符串形式。
     *
     * <p>示例：{@code "0AXFXR7X8PWGS"}（13 字符、字典序与生成时间一致）。
     *
     * @return TSID 字符串
     */
    @Override
    public String nextIdStr() {
        return tsidFactory.create().toString();
    }

    /**
     * 生成原始 {@link Tsid} 对象。
     *
     * @return TSID 对象，可通过 {@link Tsid#getInstant()} 反查生成时刻
     */
    public Tsid nextTsid() {
        return tsidFactory.create();
    }
}
