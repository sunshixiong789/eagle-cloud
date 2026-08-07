package com.eagle.system.message.announcement.application.service;

import com.eagle.system.message.announcement.domain.model.Announcement;
import com.eagle.system.message.announcement.domain.repository.AnnouncementRepository;
import com.eagle.system.message.announcement.infrastructure.cache.AnnouncementCache;
import com.eagle.system.message.announcement.interfaces.dto.AnnouncementAdminView;
import com.eagle.system.message.announcement.interfaces.dto.PublishAnnouncementRequest;
import com.eagle.system.message.announcement.domain.model.AnnouncementErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 公告后台管理服务（发布/撤回/列表）。
 *
 * <p>发布与撤回均：
 * <ol>
 *   <li>持久化到 DB（事务中）</li>
 *   <li>主动失效 Redis 缓存（事务后）</li>
 *   <li>注册 {@code AnnouncementPublishedEvent}，由
 *       {@code AnnouncementBroadcastPublisher} AFTER_COMMIT 触发跨实例 WebSocket 广播</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementAdminApplicationService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCache announcementCache;

    @Transactional
    public Announcement publish(PublishAnnouncementRequest req) {
        LocalDateTime publishTime = req.publishTime() == null ? LocalDateTime.now() : req.publishTime();
        Announcement a = Announcement.publish(
                req.category(), req.title(), req.content(),
                req.targetType(), req.targetFilter(),
                publishTime, req.expireTime()
        );
        Announcement saved = announcementRepository.save(a);
        // 广播事件由 Announcement#onPostPersist (@PostPersist) 注册，Spring Data 在 save() 期间发布——
        // 不能在此 save 之后手动 registerEvent，那样发生在事件抽取之后会永不发布。
        announcementCache.invalidateActiveCache();
        log.info("announcement published: id={}, category={}, target={}, publishTime={}",
                saved.getId(), saved.getCategory(), saved.getTargetType(), publishTime);
        return saved;
    }

    @Transactional
    public void revoke(Long announcementId) {
        Announcement a = announcementRepository.findById(announcementId)
                .orElseThrow(() -> AnnouncementErrorCode.ANNOUNCEMENT_NOT_FOUND.toNotFoundException());
        a.revoke();
        announcementRepository.save(a);
        announcementCache.invalidateActiveCache();
        log.info("announcement revoked: id={}", announcementId);
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementAdminView> listAll(Pageable pageable) {
        return announcementRepository.findAllByOrderByPublishTimeDesc(pageable)
                .map(AnnouncementAdminView::of);
    }
}
