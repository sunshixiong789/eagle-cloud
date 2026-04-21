package com.eagle.system.upms.domain.repository;

import com.eagle.system.upms.domain.model.Dept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 部门 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface DeptRepository extends JpaRepository<Dept, Long> {

}