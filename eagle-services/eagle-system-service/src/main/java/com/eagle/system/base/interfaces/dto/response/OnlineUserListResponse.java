package com.eagle.system.base.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 在线用户列表响应
 */
@Data
@AllArgsConstructor
public class OnlineUserListResponse {

    /**
     * 在线用户总数
     */
    private int totalCount;

    /**
     * 在线用户列表
     */
    private List<OnlineUserResponse> users;
}
