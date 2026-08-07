package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信服务商配置属性。
 *
 * <p>对应 application.yml 中 {@code eagle.message.sms} 前缀。该前缀原属已移除的
 * eagle-notification-starter，starter 下线后配置成了无人绑定的孤儿配置——本类接管它，
 * 保持 yml 与环境变量不变。
 *
 * <p>当前仅实现手拉手（{@code hnsls}）HTTP 网关。{@code provider} 为其他值时
 * {@link com.eagle.auth.core.infrastructure.external.SmsServiceImpl#isConfigured()}
 * 返回 false，验证码回落到打印日志的开发态兜底。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.message.sms")
public class SmsProperties {

    /**
     * 短信服务商标识。当前仅支持 {@code hnsls}，留空或其他值表示不做真实下发。
     */
    private String provider = "";

    /**
     * 短信签名，会以 {@code 【签名】} 形式拼在内容前。
     */
    private String signName = "";

    /**
     * 手拉手网关账号 name。
     */
    private String username = "";

    /**
     * 手拉手网关密码，用于生成 key（{@code md5(md5(password) + seed)}）。
     */
    private String password = "";

    /**
     * 验证码短信内容模板，{@code {code}} 为占位符。
     */
    private String contentTemplate = "您的验证码是{code}，5分钟内有效。";

    /**
     * 手拉手下行提交接口地址。
     */
    private String sendUrl = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";

    /**
     * 表单编码，需与接口地址匹配（UTF-8 接口用 UTF-8，GBK 接口用 GBK）。
     */
    private String charset = "UTF-8";

    /**
     * 连接超时，单位毫秒。
     */
    private int connectTimeoutMs = 5000;

    /**
     * 读取响应超时，单位毫秒。
     */
    private int readTimeoutMs = 10000;

    /**
     * 手拉手凭据是否齐全（账号、密码、签名三者缺一不可）。
     */
    public boolean isHnslsCredentialComplete() {
        return isNotBlank(username) && isNotBlank(password) && isNotBlank(signName);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
