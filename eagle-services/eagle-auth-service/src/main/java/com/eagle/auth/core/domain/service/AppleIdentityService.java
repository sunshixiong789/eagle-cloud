package com.eagle.auth.core.domain.service;

/**
 * Apple identity token 验证端口。
 */
public interface AppleIdentityService {

    /**
     * 验证 Apple 签名、issuer、audience、时效和 nonce。
     */
    AppleIdentity verify(String identityToken, String nonce);

    /**
     * 服务端验签后的可信 Apple 身份。
     */
    record AppleIdentity(String subject, String email) {
    }
}
