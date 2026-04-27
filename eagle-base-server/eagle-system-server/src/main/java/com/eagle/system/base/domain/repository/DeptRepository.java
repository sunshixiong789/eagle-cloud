package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.Dept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * 部门 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface DeptRepository extends JpaRepository<Dept, Long> {

    /**
     * 根据部门路径前缀查询所有子部门 ID（包含自身）。
     *
     * @param path 部门路径前缀，如 {@code /1/2/}
     * @return 部门 ID 集合
     */
    @Query("SELECT d.id FROM Dept d WHERE d.deptPath LIKE CONCAT(:path, '%')")
    Set<Long> findIdsByDeptPathStartingWith(@Param("path") String path);

    /**
     * 根据父部门 ID 查询所有直接子部门 ID。
     *
     * @param parentId 父部门 ID
     * @return 子部门 ID 集合
     */
    @Query("SELECT d.id FROM Dept d WHERE d.parentId = :parentId")
    Set<Long> findIdsByParentId(@Param("parentId") Long parentId);
}