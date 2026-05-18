package com.eagle.system.base.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务注册状态响应
 */
@Data
@Builder
public class ServiceStatusResponse {

    /** 服务 ID（注册中心名称） */
    private String serviceId;

    /** 显示名称（来自 metadata.spring-doc-name，或 serviceId） */
    private String displayName;

    /**
     * 服务状态
     * <ul>
     *   <li>UP — 有健康实例</li>
     *   <li>DOWN — 无实例（未注册或全部下线）</li>
     * </ul>
     */
    private String status;

    /** 当前健康实例数 */
    private int healthyCount;

    /** 实例列表 */
    private List<ServiceInstanceInfo> instances;
}
