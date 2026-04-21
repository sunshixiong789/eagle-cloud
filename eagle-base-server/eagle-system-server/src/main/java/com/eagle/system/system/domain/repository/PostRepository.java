package com.eagle.system.system.domain.repository;

import com.eagle.system.domain.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 岗位信息表 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 根据岗位编码查询岗位
     *
     * @param postCode 岗位编码
     * @return 岗位信息
     */
    Optional<Post> findByPostCode(String postCode);
}