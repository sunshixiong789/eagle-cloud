package com.eagle.encrypt.service;

/**
 * 字段级加解密服务接口。
 *
 * @author eagle
 */
public interface EncryptionService {

    /**
     * 加密明文字符串。
     *
     * @param plaintext 明文，null 时返回 null
     * @return Base64 编码的密文
     */
    String encrypt(String plaintext);

    /**
     * 解密密文字符串。
     *
     * @param ciphertext Base64 编码的密文，null 时返回 null
     * @return 明文
     */
    String decrypt(String ciphertext);
}
