package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户仓储接口
 * <p>
 * 直接使用 Spring Data JPA Repository，无需额外的领域仓储抽象层
 *
 * @author eagle
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 通过用户名查找用户。
     * <p>不走缓存：User 含 lazy {@code roleIds} 集合，缓存实体会触发反序列化时
     * 的 LazyInitializationException。如确需缓存，应缓存 DTO 而不是实体。
     *
     * @param username 用户名
     * @return 用户实体
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查是否存在指定 accountId 的用户
     *
     * @param accountId 认证账号 ID
     * @return 是否存在
     */
    boolean existsByAccountId(Long accountId);

    /**
     * 通过 accountId 查找用户
     *
     * @param accountId 认证账号 ID
     * @return 用户实体
     */
    Optional<User> findByAccountId(Long accountId);

    /**
     * 通过 accountId 查找用户并预加载角色 ID 集合（用于认证，不走缓存）
     *
     * @param accountId 认证账号 ID
     * @return 用户实体（roleIds 已初始化）
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roleIds WHERE u.accountId = :accountId")
    Optional<User> findByAccountIdWithRoles(Long accountId);

    /**
     * 通过邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    Optional<User> findByEmail(String email);

    /**
     * 用户列表分页查询（CQRS 读投影）
     * <p>
     * 只加载列表所需字段，避免触发 Address 等值对象的加载。
     * 使用 @Query 别名映射到 {@link UserSummary} 接口投影。
     *
     * @param pageable 分页参数
     * @return 用户摘要投影分页结果
     */
    @Query("SELECT u.id AS id, u.username AS username, " +
            "u.email AS email, " +
            "u.profile.name AS fullName, u.createTime AS createTime FROM User u")
    Page<UserSummary> findUserSummaries(Pageable pageable);

    /**
     * 统计指定时间之后创建的用户数（用于仪表盘近7天新增）
     *
     * @param since 起始时间
     * @return 用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createTime >= :since")
    Long countByCreateTimeSince(@Param("since") LocalDateTime since);

    /**
     * 查询拥有指定角色的用户（分页）
     * 通过 ElementCollection sys_user_role 关联查询
     *
     * @param roleId   角色 ID
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roleIds rid WHERE rid = :roleId")
    Page<User> findByRoleId(@Param("roleId") Long roleId, Pageable pageable);
}
