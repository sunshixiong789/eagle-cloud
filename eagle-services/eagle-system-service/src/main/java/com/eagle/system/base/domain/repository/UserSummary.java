package com.eagle.system.base.domain.repository;

import java.time.LocalDateTime;

/**
 * 用户列表查询投影（CQRS 读模型）
 *
 * @author sunshixiong
 */
public interface UserSummary {

    Long getId();

    String getUsername();

    String getEmail();

    String getFullName();

    LocalDateTime getCreateTime();
}
