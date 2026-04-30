package com.eagle.system.auth.domain.repository;

import com.eagle.system.auth.domain.model.OAuthClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OAuth2 客户端仓储接口
 *
 * @author sunshixiong
 */
@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClient, Long>,
        JpaSpecificationExecutor<OAuthClient> {

    /**
     * 通过客户端 ID 查找
     *
     * @param clientId 客户端 ID
     * @return 客户端实体
     */
    Optional<OAuthClient> findByClientId(String clientId);

    /**
     * 判断客户端 ID 是否已存在
     *
     * @param clientId 客户端 ID
     * @return 是否存在
     */
    boolean existsByClientId(String clientId);
}
