package com.eagle.auth.infrastructure.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序 SDK 配置
 *
 * @author sunshixiong
 */
@Configuration
@RequiredArgsConstructor
public class WechatMiniProgramConfig {

    private final WechatMiniProgramProperties properties;

    /**
     * 注册 WxMaService Bean，供 {@link com.eagle.auth.infrastructure.external.WechatMiniProgramServiceImpl} 使用
     */
    @Bean
    public WxMaService wxMaService() {
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(properties.getAppId());
        config.setSecret(properties.getAppSecret());
        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
