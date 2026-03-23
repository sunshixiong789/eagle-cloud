package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.entity.DictItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DictItemRepository extends JpaRepository<DictItemEntity, Long> {
    List<DictItemEntity> findByDictId(Long dictId);
}
