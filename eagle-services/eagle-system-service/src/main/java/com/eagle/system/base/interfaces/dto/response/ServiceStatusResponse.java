package com.eagle.system.base.interfaces.dto.response;

import java.util.List;

/**
 * 服务注册状态响应（含 actuator 实时指标）
 *
 * @param serviceId     服务 ID（注册中心名称）
 * @param displayName   显示名称（来自 metadata.spring-doc-name，或 serviceId）
 * @param status        注册中心状态：UP（有健康实例）/ DOWN（无实例，未注册或全部下线）
 * @param healthStatus  actuator /health 实际返回的状态（UP/DOWN/OUT_OF_SERVICE/UNKNOWN），探测失败时为 null
 * @param healthyCount  当前健康实例数
 * @param instances     实例列表
 * @param cpuUsage      CPU 使用率（0.0 ~ 1.0），取自 actuator metrics system.cpu.usage，探测失败时为 null
 * @param memUsed       JVM 已用内存（字节），取自 actuator metrics jvm.memory.used，探测失败时为 null
 * @param memMax        JVM 最大内存（字节），取自 actuator metrics jvm.memory.max，探测失败时为 null
 */
public record ServiceStatusResponse(
        String serviceId,
        String displayName,
        String status,
        String healthStatus,
        int healthyCount,
        List<ServiceInstanceInfo> instances,
        Double cpuUsage,
        Long memUsed,
        Long memMax
) {
}
