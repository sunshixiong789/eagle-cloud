package com.eagle.system.upms.domain.repository;

import com.eagle.system.upms.domain.model.Dict;
import com.eagle.system.upms.domain.model.enums.DictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 字典类型 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface DictRepository extends JpaRepository<Dict, Long> {

    /**
     * 根据字典类型查询字典
     *
     * @param dictType 字典类型枚举
     * @return 字典实体（可能为空）
     */
    Optional<Dict> findByDictType(DictType dictType);

    /**
     * 根据多个字典类型批量查询
     *
     * @param dictTypes 字典类型集合
     * @return 字典列表
     */
    List<Dict> findByDictTypeIn(Collection<DictType> dictTypes);
}