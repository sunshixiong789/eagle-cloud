package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.AppleCredentialCipher;
import com.eagle.encrypt.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 使用 eagle-encrypt-starter 加密 Apple refresh token。 */
@Service
@RequiredArgsConstructor
public class AppleCredentialCipherImpl implements AppleCredentialCipher {

    private final ObjectProvider<EncryptionService> encryptionServiceProvider;

    @Override
    public String encrypt(String plaintext) {
        return encryptionService().encrypt(plaintext);
    }

    @Override
    public String decrypt(String ciphertext) {
        return encryptionService().decrypt(ciphertext);
    }

    private EncryptionService encryptionService() {
        EncryptionService service = encryptionServiceProvider.getIfAvailable();
        if (service == null) {
            throw AuthErrorCode.APPLE_NOT_CONFIGURED.toDomainException();
        }
        return service;
    }
}
