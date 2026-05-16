package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;

import java.time.LocalDateTime;

/**
 * 日志列表查询投影（CQRS 读模型）。
 *
 * <p>避免加载 {@code params / result / exception} 等 TEXT 字段，
 * 仅返回前端列表展示所需的最小字段集——单条记录体积可下降 90%+，
 * 大列表响应吞吐显著提升。</p>
 *
 * <p>使用 Spring Data JPA 接口投影 + {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor#findBy}
 * 流式 API，编译期类型安全，无需手动映射。</p>
 *
 * @author sunshixiong
 */
public interface LogSummary {

    Long getId();

    LogType getLogType();

    String getTitle();

    Long getUserId();

    String getUsername();

    String getRemoteAddr();

    String getRequestUri();

    String getMethod();

    Long getTime();

    String getServiceId();

    LogStatus getStatus();

    LocalDateTime getCreateTime();
}
