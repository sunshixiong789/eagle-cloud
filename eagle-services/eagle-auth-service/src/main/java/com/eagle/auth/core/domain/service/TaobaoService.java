package com.eagle.auth.core.domain.service;

/**
 * 淘宝身份解析服务：用百川授权产物换取稳定的淘宝 openUid。
 *
 * @author sunshixiong
 */
public interface TaobaoService {

    /**
     * 解析淘宝稳定 openUid。
     *
     * <p>百川 native SDK 授权返回的是 <strong>access token</strong>（非授权码），优先用它凭 TOP
     * session 调 {@code taobao.openuid.get} 直取 openUid；{@code tbAuthCode} 路径保留作兜底
     * （将来 Web/H5 授权码流可复用，调 {@code taobao.top.auth.token.create} 换 token_result）。
     * 两者皆空时抛 {@code TAOBAO_AUTH_REQUIRED}。
     *
     * @param tbAccessToken 百川 SDK 授权返回的淘宝 access token（作为 TOP session），主路径
     * @param tbAuthCode    TOP 授权码，兜底路径；可为 null
     * @return 稳定 openUid
     */
    String resolveOpenUid(String tbAccessToken, String tbAuthCode);
}
