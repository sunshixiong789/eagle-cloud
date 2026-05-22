package com.eagle.auth.domain.service;

/**
 * 手机号一键登录服务（领域层接口）
 * <p>
 * 由运营商（中国移动 / 联通 / 电信）或聚合 SDK（阿里云号码认证 / 网易盾 / 极光）颁发 access_token，
 * 客户端将 token 提交到本接口，由基础设施层适配器调用运营商网关换取真实手机号。
 * <p>
 * 与短信验证码登录的本质差异：
 * <ul>
 *   <li>短信登录：用户主动输入手机号 + 验证码，服务端校验</li>
 *   <li>一键登录：运营商在网络层识别 SIM 卡持有人，颁发短期 token，服务端凭 token 反查手机号</li>
 * </ul>
 *
 * @author sunshixiong
 */
public interface PhoneOneClickService {

    /**
     * 校验运营商颁发的一键登录 access_token，并换取真实手机号
     *
     * @param accessToken 运营商 / SDK 颁发的短期访问凭证
     * @return 真实手机号（11 位）
     */
    String verifyAndGetPhone(String accessToken);
}
