package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手拉手短信服务配置属性。
 *
 * <p>对应 application.yml 中的 {@code eagle.sms.hnsls} 前缀配置。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.sms.hnsls")
public class HnslsSmsProperties {

    /**
     * 网关账号 name。
     */
    private String username = "";

    /**
     * 网关密码，用于生成 key，不会明文提交。
     */
    private String password = "";

    /**
     * 短信签名，不含外层中文方括号。
     */
    private String signName = "";

    /**
     * 验证码短信内容模板，使用 {code} 作为验证码占位符。
     */
    private String contentTemplate = "您的验证码是{code}，5分钟内有效。";

    /**
     * 下行提交接口地址，默认使用 UTF-8 域名地址。
     */
    private String sendUrl = "https://xapi.hnsls.com.cn/eums/sms/utf8/send.do";

    /**
     * 表单编码，需与接口地址匹配，可选 UTF-8 / GBK。
     */
    private String charset = "UTF-8";
}
