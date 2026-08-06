package com.eagle.system.base.interfaces.dto.response;

import java.util.Map;

/**
 * 服务实例信息
 *
 * @param instanceId 实例 ID
 * @param host       主机地址
 * @param port       端口
 * @param metadata   实例元数据（来自注册中心）
 */
public record ServiceInstanceInfo(
        String instanceId,
        String host,
        int port,
        Map<String, String> metadata
) {
}
