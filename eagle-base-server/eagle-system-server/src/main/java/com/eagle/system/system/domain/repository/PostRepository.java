package com.eagle.system.system.domain.repository;

import com.eagle.eagle.system.domain.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * 岗位信息表 Repository
 *
 * @author sunshixiong
 */
@Repository
@RepositoryRestResource(path = "post")
public interface PostRepository extends JpaRepository<Post, Long> {

}