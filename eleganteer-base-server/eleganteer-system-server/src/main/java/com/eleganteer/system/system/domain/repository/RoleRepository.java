package com.eleganteer.system.system.domain.repository;

import com.eleganteer.eleganteer.system.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * 角色 Repository
 *
 * @author sunshixiong
 */
@Repository
@RepositoryRestResource(path = "role")
public interface RoleRepository extends JpaRepository<Role, Long> {

}