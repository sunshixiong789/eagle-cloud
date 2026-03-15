package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 日志 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface LogRepository extends JpaRepository<SysLog, Long> {

}