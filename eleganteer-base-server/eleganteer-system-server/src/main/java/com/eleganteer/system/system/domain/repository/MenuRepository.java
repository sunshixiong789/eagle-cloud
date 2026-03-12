package com.eleganteer.system.system.domain.repository;

import com.eleganteer.eleganteer.system.domain.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

/**
 * 菜单 Repository
 *
 * @author sunshixiong
 */
@Repository
@RepositoryRestResource(path = "menu")
public interface MenuRepository extends JpaRepository<Menu, Long> {

}