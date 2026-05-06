package com.eagle.system.auth.domain.port;

/**
 * 授权信息 DTO
 * <p>
 * auth 域通过 {@link AuthorizationPort} 获取此对象，
 * 包含构建 JWT claims 所需的展示信息。
 *
 * @author sunshixiong
 */
public record AuthorizationInfo(
        // 真实姓名（JWT claim）
        String name
) {

    /**
     * 空授权信息（新注册用户尚未设置资料时使用）
     */
    public static AuthorizationInfo empty() {
        return new AuthorizationInfo(null);
    }
}
