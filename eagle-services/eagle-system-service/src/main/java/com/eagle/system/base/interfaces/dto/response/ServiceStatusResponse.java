package com.eagle.system.base.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务注册状态响应（含 actuator 实时指标）
 */
@Data
@Builder
public class ServiceStatusResponse {

    /** 服务 ID（注册中心名称） */
    private String serviceId;

    /** 显示名称（来自 metadata.spring-doc-name，或 serviceId） */
    private String displayName;

    /**
     * 注册中心状态
     * <ul>
     *   <li>UP — 有健康实例</li>
     *   <li>DOWN — 无实例（未注册或全部下线）</li>
     * </ul>
     */
    private String status;

    /**
     * actuator /health 实际返回的状态（UP/DOWN/OUT_OF_SERVICE/UNKNOWN）
     * <p>探测失败时为 null。
     */
    private String healthStatus;

    /** 当前健康实例数 */
    private int healthyCount;

    /** 实例列表 */
    private List<ServiceInstanceInfo> instances;

    /**
     * CPU 使用率（0.0 ~ 1.0），探测失败时为 null。
     * 取自 actuator metrics system.cpu.usage。
     */
    private Double cpuUsage;

    /**
     * JVM 已用内存（字节），探测失败时为 null。
     * 取自 actuator metrics jvm.memory.used。
     */
    private Long memUsed;

    /**
     * JVM 最大内存（字节），探测失败时为 null。
     * 取自 actuator metrics jvm.memory.max。
     */
    private Long memMax;
}
