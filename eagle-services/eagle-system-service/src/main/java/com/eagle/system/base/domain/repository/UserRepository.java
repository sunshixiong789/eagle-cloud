package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import org.springframework.cache.annotation.Cacheable;
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
 *
 * @author 孙士雄
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Cacheable(value = "USER_NAME", key = "#username")
    Optional<User> findByUsername(String username);

    boolean existsByAccountId(Long accountId);

    Optional<User> findByAccountId(Long accountId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u.id AS id, u.username AS username, " +
            "u.email AS email, " +
            "u.profile.name AS fullName, u.createTime AS createTime FROM User u")
    Page<UserSummary> findUserSummaries(Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createTime >= :since")
    Long countByCreateTimeSince(@Param("since") LocalDateTime since);
}
