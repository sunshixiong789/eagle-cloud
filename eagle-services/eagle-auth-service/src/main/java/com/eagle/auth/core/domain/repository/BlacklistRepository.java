package com.eagle.auth.core.domain.repository;

import com.eagle.auth.core.domain.model.Blacklist;
import com.eagle.auth.core.domain.model.enums.BlacklistType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 黑名单仓储
 *
 * @author sunshixiong
 */
@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    /**
     * 按类型和值查找黑名单记录。
     *
     * @param type  黑名单类型
     * @param value 黑名单值
     * @return 黑名单记录，若不存在返回空
     */
    Optional<Blacklist> findByTypeAndValue(BlacklistType type, String value);

    /**
     * 按类型和值模糊查询，支持分页。
     *
     * @param type     黑名单类型
     * @param value    黑名单值（模糊匹配）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Blacklist> findByTypeAndValueContaining(BlacklistType type, String value, Pageable pageable);

    /**
     * 按类型查询，支持分页。
     *
     * @param type     黑名单类型
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Blacklist> findByType(BlacklistType type, Pageable pageable);

    /**
     * 启动期全量加载未过期的黑名单记录，用于 Redis 缓存预热。
     *
     * @param now 当前时间
     * @return 未过期的黑名单列表
     */
    @Query(value = "SELECT * FROM auth_blacklist " +
            "WHERE expires_at IS NULL OR expires_at > :now",
            nativeQuery = true)
    List<Blacklist> findAllActiveForCacheWarmup(@Param("now") LocalDateTime now);
}
