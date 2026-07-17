package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 审核白名单固定验证码配置（App Store 提审用）。
 *
 * <p>对应 application.yml 中 {@code eagle.auth.sms-mock} 前缀。白名单内的手机号
 * 不发送真实短信，直接使用固定验证码 {@link #code} 完成登录/绑定校验，用于
 * 苹果审核人员无法接收国内短信的场景——提审时把测试手机号填入
 * {@code SMS_MOCK_PHONES} 并写进 App Review 备注即可。
 *
 * <p><strong>仅对白名单手机号生效</strong>：{@link #phones} 为空（默认）时功能完全关闭，
 * 其余手机号始终走真实短信流程，固定验证码对其无效，生产环境开启不影响正常用户安全。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.auth.sms-mock")
public class SmsMockProperties {

    /**
     * 固定验证码，默认 {@code 123456}。
     */
    private String code = "123456";

    /**
     * 审核白名单手机号（逗号分隔注入）。为空时固定验证码功能整体关闭。
     */
    private Set<String> phones = new LinkedHashSet<>();

    /**
     * 该手机号是否命中审核白名单（白名单非空、验证码非空且包含该号码）。
     */
    public boolean isMockPhone(String phone) {
        return code != null && !code.isBlank() && phone != null && phones.contains(phone);
    }
}
