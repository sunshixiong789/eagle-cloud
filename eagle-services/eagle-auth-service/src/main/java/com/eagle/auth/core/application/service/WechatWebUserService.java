package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.WechatChannel;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.WechatWebService.WechatWebUserInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 微信用户应用服务。
 *
 * <p>提供四渠道（小程序 / App / PC 扫码 / H5）统一的微信账号查找：
 * 本渠道 openid 优先，unionid 归并兜底（同一微信主体已在别的渠道完成过
 * 手机号验证时，补绑本渠道 openid 直登，不重复验手机号）。
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
     * 四渠道统一查找微信账号（不创建）。
     *
     * <p>顺序：
     * <ol>
     *   <li>本渠道 openid 命中 → 返回（附带 unionid 时补写）</li>
     *   <li>unionid 命中 → 补绑本渠道 openid 后返回（跨渠道归并直登）</li>
     *   <li>都未命中 → empty（由调用方走 binding_required 流程）</li>
     * </ol>
     *
     * @param channel 微信渠道
     * @param openid  本渠道 openid
     * @param unionid unionid（可空）
     * @return 命中的账号；未命中 empty
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<Account> findWechatAccount(WechatChannel channel,
                                               String openid, String unionid) {
        Optional<Account> byOpenid = findByChannelOpenid(channel, openid);
        if (byOpenid.isPresent()) {
            Account existing = byOpenid.get();
            // 历史数据可能缺 unionid，命中后补写
            if (unionid != null && !unionid.isBlank()) {
                bindChannelOpenid(existing, channel, openid, unionid);
                accountRepository.save(existing);
            }
            return Optional.of(existing);
        }

        if (unionid != null && !unionid.isBlank()) {
            Optional<Account> byUnionid = accountRepository.findByWechatBindingUnionid(unionid);
            if (byUnionid.isPresent()) {
                Account existing = byUnionid.get();
                bindChannelOpenid(existing, channel, openid, unionid);
                accountRepository.save(existing);
                log.info("微信登录：通过 unionid 归并账号, accountId: {}, channel: {}",
                        existing.getId(), channel);
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    /**
     * 查找或创建微信开放平台账号（网页扫码 / H5 浏览器 session 流程专用）。
     *
     * <p>token 端点的四个 grant 已改为 binding_required + social_bind，
     * 不再调用本方法的创建分支；web/H5 保留「先建账号 + 引导绑手机 + 归并」路径。
     *
     * @param info 微信用户信息
     * @return Account 实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Account findOrCreateWechatWebAccount(WechatWebUserInfo info) {
        WechatChannel channel = webChannelOf(info.channel());
        Optional<Account> found = findWechatAccount(channel, info.openid(), info.unionid());
        if (found.isPresent()) {
            return found.get();
        }

        Account saved = accountRepository.save(createWechatWebAccount(info));
        log.info("微信 Web 登录:自动注册新账号, username: {}, channel: {}",
                saved.getUsername(), info.channel());
        return saved;
    }

    /**
     * Web 流程的字符串渠道转枚举（"app" / "pc" 属开放平台，其余按 H5 处理）。
     */
    public static WechatChannel webChannelOf(String channel) {
        return switch (channel) {
            case "app" -> WechatChannel.APP;
            case "pc" -> WechatChannel.PC;
            default -> WechatChannel.H5;
        };
    }

    private Optional<Account> findByChannelOpenid(WechatChannel channel, String openid) {
        return switch (channel) {
            case MINI_PROGRAM -> accountRepository.findByWechatBindingOpenid(openid);
            case APP, PC -> accountRepository.findByWechatBindingWebOpenid(openid);
            case H5 -> accountRepository.findByWechatBindingMpOpenid(openid);
        };
    }

    private void bindChannelOpenid(Account account, WechatChannel channel,
                                   String openid, String unionid) {
        switch (channel) {
            case MINI_PROGRAM -> account.bindWechat(openid, unionid);
            case APP, PC -> account.bindWechatWeb(openid, unionid);
            case H5 -> account.bindWechatH5(openid, unionid);
        }
    }

    private Account createWechatWebAccount(WechatWebUserInfo info) {
        if (webChannelOf(info.channel()).isOpenPlatform()) {
            return Account.createFromWechatWeb(
                    info.openid(), info.unionid(), info.nickname(), info.avatar());
        }
        return Account.createFromWechatH5(
                info.openid(), info.unionid(), info.nickname(), info.avatar());
    }
}
