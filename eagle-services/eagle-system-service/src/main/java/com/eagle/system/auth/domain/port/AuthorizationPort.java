package com.eagle.system.auth.domain.port;

import java.util.Optional;

/**
 * 授权信息查询驱动端口（Driven Port）
 * <p>
 * auth 领域通过此端口获取用户的角色码和展示信息（部门等），用于构建 JWT claims。
 * <p>
 * 在单体架构中，system 基础设施层提供本地实现（{@code AuthorizationAdapter}）。
 * 拆分为微服务时，auth 基础设施层提供远程实现（HTTP/gRPC），无需修改此接口及其调用方。
 *
 * @author sunshixiong
 */
public interface AuthorizationPort {

    /**
     * 按账号 ID 查询授权信息
     *
     * @param accountId 账号 ID（与 User.accountId 关联）
     * @return 授权信息，用户不存在时返回 empty
     */
    Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId);
}
