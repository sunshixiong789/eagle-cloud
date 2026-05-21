package com.eagle.system.message.domain.repository;

import com.eagle.system.message.domain.model.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 站内消息仓储。
 *
 * @author sunshixiong
 */
@Repository
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {

    Page<UserMessage> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByBizKey(String bizKey);

    @Modifying
    @Query("UPDATE UserMessage m SET m.isRead = true WHERE m.userId = :userId AND m.isRead = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}
