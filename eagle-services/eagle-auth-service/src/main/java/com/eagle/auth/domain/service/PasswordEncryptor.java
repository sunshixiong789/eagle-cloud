package com.eagle.auth.domain.service;

/**
 * 密码加密器（领域层接口）
 * <p>
 * 定义密码加密的领域能力，具体实现由基础设施层提供
 * <p>
 * 这样做的好处：
 * <ul>
 *   <li>领域层不依赖具体的加密技术（如 BCrypt）</li>
 *   <li>可以方便地切换加密算法</li>
 *   <li>便于单元测试（可以使用 Mock 实现）</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
public interface PasswordEncryptor {

    /**
     * 加密明文密码
     *
     * @param plainPassword 明文密码
     * @return 加密后的密码
     */
    String encrypt(String plainPassword);

    /**
     * 验证明文密码是否匹配加密密码
     *
     * @param plainPassword     明文密码
     * @param encryptedPassword 加密后的密码
     * @return true 表示匹配
     */
    boolean matches(String plainPassword, String encryptedPassword);
}
