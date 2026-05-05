package com.eagle.encrypt.service;

import com.eagle.encrypt.properties.EncryptProperties;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/CBC/PKCS5Padding 字段级加密实现。
 *
 * <p>存储格式：Base64(IV[16 bytes] + CipherText)
 *
 * @author eagle
 */
@Slf4j
public class AesEncryptionService implements EncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    private final byte[] keyBytes;

    public AesEncryptionService(EncryptProperties properties) {
        String secretKey = properties.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "eagle.encrypt.secret-key 未配置，请通过环境变量 EAGLE_ENCRYPT_SECRET_KEY 注入");
        }
        this.keyBytes = deriveKey(secretKey);
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(iv));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = concat(iv, cipherText);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("字段加密失败", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("字段解密失败", e);
        }
    }

    private static byte[] deriveKey(String secretKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("密钥派生失败", e);
        }
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
