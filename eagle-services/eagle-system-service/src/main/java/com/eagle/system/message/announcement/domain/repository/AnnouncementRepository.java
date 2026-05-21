package com.eagle.system.message.announcement.domain.repository;

import com.eagle.system.message.announcement.domain.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告仓储。
 *
 * @author sunshixiong
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** 加载缓存所需的全部活跃公告（未撤回、已发布、未过期），按 publish_time 升序。 */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.revoked = false
              AND a.publishTime <= :now
              AND (a.expireTime IS NULL OR a.expireTime > :now)
            ORDER BY a.publishTime ASC
            """)
    List<Announcement> findAllActive(@Param("now") LocalDateTime now);

    /** 后台分页列表（含已撤回/已过期）。 */
    Page<Announcement> findAllByOrderByPublishTimeDesc(Pageable pageable);
}
