package com.eleganteer.system.system.domain.repository;

import com.eleganteer.eleganteer.system.domain.model.Dept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * 部门 Repository
 *
 * @author sunshixiong
 */
@Repository
@RepositoryRestResource(path = "dept")
public interface DeptRepository extends JpaRepository<Dept, Long> {

}