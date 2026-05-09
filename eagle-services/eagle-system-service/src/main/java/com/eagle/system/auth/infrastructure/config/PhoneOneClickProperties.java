package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手机号一键登录配置属性
 * <p>
 * 对应 application.yml 中的 {@code eagle.auth.one-click} 前缀配置。
 * 当前实现内置 {@code mock} 与 {@code aliyun} 两种提供方：
 * <ul>
 *   <li>{@code mock}（默认）：开发环境直接将 access_token 视为手机号，便于联调</li>
 *   <li>{@code aliyun}：调用阿里云号码认证（dypnsapi）网关换取真实手机号</li>
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
     * 提供方：mock / aliyun
     */
    private String provider = "mock";

    /**
     * 阿里云号码认证 endpoint，默认 dypnsapi.aliyuncs.com
     */
    private String endpoint = "dypnsapi.aliyuncs.com";

    /**
     * AccessKey ID（阿里云）
     */
    private String accessKeyId = "";

    /**
     * AccessKey Secret（阿里云）
     */
    private String accessKeySecret = "";
}
