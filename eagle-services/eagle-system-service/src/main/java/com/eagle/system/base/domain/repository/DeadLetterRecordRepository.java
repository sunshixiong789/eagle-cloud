package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.DeadLetterRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 死信记录 Repository。
 *
 * @author sunshixiong
 */
@Repository
public interface DeadLetterRecordRepository extends JpaRepository<DeadLetterRecord, Long> {

    /**
     * 按 eventId 查找最近一条记录,避免同一事件被反复落库为多条。
     *
     * <p>注意:eventId 不是 unique 索引(允许 null 防御反序列化失败),
     * 因此不能用 unique constraint 兜底,这里仅做"最近一次"查询。
     */
    Optional<DeadLetterRecord> findFirstByEventIdOrderByCreateTimeDesc(String eventId);
}
