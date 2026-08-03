package com.eagle.auth.core.domain.repository;

import com.eagle.auth.core.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * 统计排除指定用户名后的账号数。
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE LOWER(a.username) <> LOWER(:username)")
    long countByUsernameNot(@Param("username") String username);

    /**
     * 通过手机号查找账号（短信登录）
     */
    Optional<Account> findByPhone(String phone);

    /**
     * 按手机号批量查找账号（跨服务按手机号解析 accountId）。
     *
     * <p>未注册的号码不会出现在结果中，调用方可用返回集合与请求集合的差集识别未注册号码。
     */
    List<Account> findByPhoneIn(Set<String> phones);

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

    /**
     * 按淘宝 openUid 查找账号（淘宝登录直登捷径）。
     */
    Optional<Account> findByTaobaoBindingOpenUid(String openUid);

    /** 按服务端验签后的 Apple subject 查找账号。 */
    Optional<Account> findByAppleBindingSubject(String subject);
}
