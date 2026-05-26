package com.eagle.auth.core.domain.repository;

import com.eagle.auth.core.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 账号仓储接口
 *
 * @author sunshixiong
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 通过用户名查找账号（账密登录）
     */
    Optional<Account> findByUsername(String username);

    /**
     * 通过手机号查找账号（短信登录）
     */
    Optional<Account> findByPhone(String phone);

    /**
     * 通过微信小程序 openid 查找账号
     */
    Optional<Account> findByWechatBindingOpenid(String openid);

    /**
     * 通过微信 unionid 查找账号（跨平台合并）
     */
    Optional<Account> findByWechatBindingUnionid(String unionid);

    /**
     * 通过微信 PC 扫码 openid 查找账号
     */
    Optional<Account> findByWechatBindingWebOpenid(String webOpenid);

    /**
     * 通过微信公众号 H5 openid 查找账号
     */
    Optional<Account> findByWechatBindingMpOpenid(String mpOpenid);
}
