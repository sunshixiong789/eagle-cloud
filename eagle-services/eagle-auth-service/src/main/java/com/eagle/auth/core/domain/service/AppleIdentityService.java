package com.eagle.auth.core.domain.service;

/**
 * Apple identity token 验证端口。
 */
public interface AppleIdentityService {

    /** 验证 identity token，并用一次性授权码向 Apple 服务端换票。 */
    AppleAuthorization authorize(
            String identityToken, String authorizationCode, String nonce);

    /** 解密并撤销账号持有的 Apple refresh token。 */
    void revokeEncryptedRefreshToken(String encryptedRefreshToken);

    /**
     * 服务端验签且完成授权码换票后的可信 Apple 身份。
     */
    record AppleAuthorization(
            String subject, String email, String encryptedRefreshToken) {
    }
}
