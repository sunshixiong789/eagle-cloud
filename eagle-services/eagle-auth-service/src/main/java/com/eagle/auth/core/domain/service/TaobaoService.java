package com.eagle.auth.core.domain.service;

/**
 * 淘宝身份解析服务：用百川授权产物换取稳定的淘宝 openUid。
 *
 * @author sunshixiong
 */
public interface TaobaoService {

    /**
     * 用 topAuthCode（百川 native 授权返回）解析淘宝 openUid。
     *
     * @param topAuthCode 百川 SDK 授权返回的 TOP 授权码
     * @return 稳定 openUid
     */
    String resolveOpenUid(String topAuthCode);
}
