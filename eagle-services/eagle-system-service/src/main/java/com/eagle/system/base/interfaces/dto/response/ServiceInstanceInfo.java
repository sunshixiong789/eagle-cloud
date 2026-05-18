package com.eagle.system.base.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 服务实例信息
 */
@Data
@Builder
public class ServiceInstanceInfo {

    /** 实例 ID */
    private String instanceId;

    /** 主机地址 */
    private String host;

    /** 端口 */
    private int port;

    /** 实例元数据（来自 Nacos 注册信息） */
    private Map<String, String> metadata;
}
