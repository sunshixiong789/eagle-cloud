package com.eagle.system.message.announcement.domain.repository;

import com.eagle.system.message.announcement.domain.model.UserAnnouncementCursor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户公告读位游标仓储。
 *
 * @author sunshixiong
 */
@Repository
public interface UserAnnouncementCursorRepository extends JpaRepository<UserAnnouncementCursor, Long> {

    Optional<UserAnnouncementCursor> findByUserId(Long userId);
}
