package com.eagle.auth.core.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手机号一键登录配置属性
 * <p>
 * 对应 application.yml 中的 {@code eagle.auth.one-click} 前缀配置。{@code provider} 决定运行时使用哪一个
 * {@link com.eagle.auth.core.infrastructure.external.provider.PhoneOneClickProvider} 实现：
 * <ul>
 *   <li>{@code mock}（默认）：开发环境直接将 access_token 视为手机号，便于联调</li>
 *   <li>{@code aliyun}：调用阿里云号码认证（dypnsapi）网关换取真实手机号</li>
 *   <li>{@code tencent}：调用腾讯云号码认证（PNSV，通过 SDK CommonClient）换取真实手机号</li>
 * </ul>
 * 生产环境必须切换为运营商或聚合 SDK 实现，并配置对应凭证。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.auth.one-click")
public class PhoneOneClickProperties {

    /**
     * 是否启用一键登录
     */
    private boolean enabled = true;

    /**
     * 提供方：mock / aliyun / tencent
     */
    private String provider = "mock";

    /**
     * 阿里云号码认证配置
     */
    private final Aliyun aliyun = new Aliyun();

    /**
     * 腾讯云号码认证配置
     */
    private final Tencent tencent = new Tencent();

    @Getter
    @Setter
    public static class Aliyun {

        /**
         * 阿里云号码认证 endpoint，默认 dypnsapi.aliyuncs.com
         */
        private String endpoint = "dypnsapi.aliyuncs.com";

        /**
         * AccessKey ID
         */
        private String accessKeyId = "";

        /**
         * AccessKey Secret
         */
        private String accessKeySecret = "";
    }

    @Getter
    @Setter
    public static class Tencent {

        /**
         * 服务区域，默认广州
         */
        private String region = "ap-guangzhou";

        /**
         * 自定义 endpoint，留空则由 SDK 按 service 自动推导（一般无需设置）
         */
        private String endpoint = "";

        /**
         * 腾讯云 API 凭证 SecretId
         */
        private String secretId = "";

        /**
         * 腾讯云 API 凭证 SecretKey
         */
        private String secretKey = "";

        /**
         * 产品代号 / service。号码认证服务默认 {@code pnsv}，按公司实际开通的腾讯云号码认证产品控制台为准
         */
        private String service = "pnsv";

        /**
         * API 版本号（路径 yyyy-MM-dd），与 service 配套
         */
        private String version = "2018-07-11";

        /**
         * 调用的 API Action 名称
         */
        private String action = "GetPhoneNumber";

        /**
         * 业务成功标识码，部分产品在响应里携带 {@code Code}/{@code Status} 字段。默认 {@code Ok}
         * （腾讯云一键登录文档约定）；不同产品可能是 {@code 0}/{@code Success}，按响应实际值调整
         */
        private String successCode = "Ok";

        /**
         * 响应 JSON 中携带手机号的字段名（不含 {@code Response} 外层），默认 {@code Mobile}
         */
        private String phoneField = "Mobile";
    }
}
