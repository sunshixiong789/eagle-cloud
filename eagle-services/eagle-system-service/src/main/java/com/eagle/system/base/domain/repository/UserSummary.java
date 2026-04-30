package com.eagle.system.base.domain.repository;

import java.time.LocalDateTime;

/**
 * 用户列表查询投影（CQRS 读模型）
 * <p>
 * 避免为列表查询加载完整 User 聚合根（含值对象 Address 等），
 * 只取前端展示所需的最小字段集，提升查询性能。
 * <p>
 * 使用 Spring Data JPA 接口投影，编译期安全，无需手动映射。
 * <p>
 * 注意：phone、locked 等认证相关字段已迁移至 auth 域的 Account 聚合。
 *
 * @author sunshixiong
 */
public interface UserSummary {

    /** 用户 ID */
    Long getId();

    /** 用户名 */
    String getUsername();

    /** 邮箱 */
    String getEmail();

    /** 部门 ID */
    Long getDeptId();

    /** 真实姓名（来自嵌入值对象 UserProfile） */
    String getFullName();

    /** 账号创建时间 */
    LocalDateTime getCreateTime();
}
