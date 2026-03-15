package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 * <p>
 * 直接使用 Spring Data JPA Repository，无需额外的领域仓储抽象层
 *
 * @author 孙士雄
 */
@Repository
@RepositoryRestResource(collectionResourceRel = "users", path = "user")
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 通过用户名查找用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Cacheable(value = "USER_NAME", key = "#username")
    Optional<User> findByUsername(String username);

    /**
     * 通过手机号查找用户
     *
     * @param phone 手机号
     * @return 用户实体
     */
    Optional<User> findByPhone(String phone);

    /**
     * 通过邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据部门 ID 查询用户列表
     *
     * @param deptId 部门 ID
     * @return 用户列表
     */
    List<User> findByDeptId(Long deptId);
}