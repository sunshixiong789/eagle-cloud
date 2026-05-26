package com.eagle.auth.core.domain.service;

/**
 * 短信服务（领域层接口）
 * <p>
 * 定义短信相关的领域能力，具体实现由基础设施层提供
 *
 * @author sunshixiong
 */
public interface SmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     */
    void sendCode(String phone);

    /**
     * 验证短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return true 表示验证通过
     */
    boolean verifyCode(String phone, String code);
}
