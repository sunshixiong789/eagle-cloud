package com.eagle.system.upms.infrastructure.schedule;

import com.eagle.system.upms.domain.repository.LogRepository;
import com.eagle.system.upms.infrastructure.config.LogCleanupProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 审计日志定期清理任务
 * <p>
 * 根据 {@link LogCleanupProperties} 配置，定期删除超过保留天数的审计日志，
 * 防止日志表无限增长影响数据库性能。
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class LogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    private final LogRepository logRepository;
    private final LogCleanupProperties properties;

    /**
     * 定期清理过期审计日志
     * <p>
     * cron 表达式和保留天数均可通过 {@code eagle.log.cleanup.*} 配置。
     * 可通过 {@code eagle.log.cleanup.enabled=false} 关闭。
     */
    @Scheduled(cron = "${eagle.log.cleanup.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredLogs() {
        if (!properties.isEnabled()) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(properties.getRetentionDays());
        int deleted = logRepository.deleteByCreateTimeBefore(cutoff);

        if (deleted > 0) {
            log.info("审计日志清理完成, 删除 {} 天前的日志 {} 条",
                    properties.getRetentionDays(), deleted);
        }
    }
}
