package com.eagle.system.base.interfaces.dto.response;

import java.util.List;

/**
 * 在线用户列表响应
 *
 * @param totalCount 在线用户总数
 * @param users      在线用户列表
 */
public record OnlineUserListResponse(
        int totalCount,
        List<OnlineUserResponse> users
) {
}
