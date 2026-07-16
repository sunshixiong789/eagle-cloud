package com.eagle.auth.core.domain.service;

/** Apple refresh token 的字段级加解密端口。 */
public interface AppleCredentialCipher {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
