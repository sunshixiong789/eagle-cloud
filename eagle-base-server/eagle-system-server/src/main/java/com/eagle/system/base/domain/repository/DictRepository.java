package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.Dict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * 字典类型 Repository
 *
 * @author sunshixiong
 */
@Repository
@RepositoryRestResource(path = "dict")
public interface DictRepository extends JpaRepository<Dict, Long> {

}