package com.eagle.auth.application.service;

import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.repository.AccountRepository;
import com.eagle.auth.domain.service.WechatWebService.WechatWebUserInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 微信 Web 端用户应用服务
 * <p>
 * 负责微信开放平台 App / Web 登录的账号查找与创建。
 * 通过 {@link AccountRepository} 直接操作 auth 域的 Account 聚合根。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class WechatWebUserService {

    private static final Logger log = LoggerFactory.getLogger(WechatWebUserService.class);

    private final AccountRepository accountRepository;

    /**
     * 查找或创建微信开放平台账号
     * <p>
     * 账号合并策略:
     * <ol>
     *   <li>优先按 unionid 查找(同一微信开放平台账号下的不同应用 unionid 相同)</li>
     *   <li>如果找到,绑定当前渠道的 openid 并返回(实现 PC 扫码与 H5 账号合并)</li>
     *   <li>如果没找到,按当前渠道的 openid 查找</li>
     *   <li>如果还没找到,创建新账号</li>
     * </ol>
     *
     * @param info 微信用户信息
     * @return Account 实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Account findOrCreateWechatWebAccount(WechatWebUserInfo info) {
        // 1. 优先按 unionid 查找(实现跨平台账号合并)
        if (info.unionid() != null && !info.unionid().isBlank()) {
            Optional<Account> byUnionid =
                    accountRepository.findByWechatBindingUnionid(info.unionid());
            if (byUnionid.isPresent()) {
                Account existing = byUnionid.get();
                // 绑定当前渠道的 openid(App、PC 或 H5)
                bindChannelOpenid(existing, info);
                accountRepository.save(existing);
                log.info("微信 Web 登录:通过 unionid 合并账号, accountId: {}, channel: {}",
                        existing.getId(), info.channel());
                return existing;
            }
        }

        // 2. 按平台 openid 查找(App、PC 扫码或 H5)
        Optional<Account> byOpenid = findByChannelOpenid(info);
        if (byOpenid.isPresent()) {
            Account existing = byOpenid.get();
            // 如果有 unionid,补充绑定(历史数据可能没有 unionid)
            if (info.unionid() != null && !info.unionid().isBlank()) {
                bindChannelOpenid(existing, info);
                accountRepository.save(existing);
            }
            return existing;
        }

        // 3. 自动创建新账号
        Account newAccount = createWechatWebAccount(info);
        Account saved = accountRepository.save(newAccount);
        log.info("微信 Web 登录:自动注册新账号, username: {}, channel: {}",
                saved.getUsername(), info.channel());
        return saved;
    }

    private Optional<Account> findByChannelOpenid(WechatWebUserInfo info) {
        if (isOpenPlatformChannel(info.channel())) {
            return accountRepository.findByWechatBindingWebOpenid(info.openid());
        }
        return accountRepository.findByWechatBindingMpOpenid(info.openid());
    }

    private void bindChannelOpenid(Account account, WechatWebUserInfo info) {
        if (isOpenPlatformChannel(info.channel())) {
            account.bindWechatWeb(info.openid(), info.unionid());
        } else {
            account.bindWechatH5(info.openid(), info.unionid());
        }
    }

    private Account createWechatWebAccount(WechatWebUserInfo info) {
        if (isOpenPlatformChannel(info.channel())) {
            return Account.createFromWechatWeb(
                    info.openid(), info.unionid(), info.nickname(), info.avatar());
        }
        return Account.createFromWechatH5(
                info.openid(), info.unionid(), info.nickname(), info.avatar());
    }

    private boolean isOpenPlatformChannel(String channel) {
        return "app".equals(channel) || "pc".equals(channel);
    }
}
