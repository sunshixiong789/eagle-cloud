package com.eagle.auth.domain.service;

/**
 * 微信服务（领域层接口）
 * <p>
 * 定义微信相关的领域能力，具体实现由基础设施层提供
 *
 * @author sunshixiong
 */
public interface WechatService {

    /**
     * 通过小程序临时登录凭证获取微信用户信息
     *
     * @param code wx.login() 返回的临时凭证
     * @return 微信用户信息
     */
    WechatUserInfo getUserInfo(String code);

    /**
     * 微信用户信息
     *
     * @param openid     用户唯一标识
     * @param unionid    用户在开放平台的唯一标识（可能为空）
     * @param sessionKey 会话密钥
     */
    record WechatUserInfo(String openid, String unionid, String sessionKey) {
    }
}
