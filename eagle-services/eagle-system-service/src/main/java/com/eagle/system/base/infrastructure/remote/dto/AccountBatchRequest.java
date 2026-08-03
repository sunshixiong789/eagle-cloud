package com.eagle.system.base.infrastructure.remote.dto;

import java.util.Set;

/** auth-service 内部批量账号快照查询请求。 */
public record AccountBatchRequest(Set<Long> accountIds) {
}
