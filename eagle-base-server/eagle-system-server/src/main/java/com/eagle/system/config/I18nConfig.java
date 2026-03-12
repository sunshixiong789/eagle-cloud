package com.eagle.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

/**
 * 国际化配置
 * <p>
 * 配置国际化消息源和语言切换拦截器。
 * 支持通过请求参数（lang）动态切换语言。
 *
 * @author 孙士雄（sunshix@seeyon.com）
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /**
     * 配置语言解析器
     * <p>
     * 使用 Session 存储用户选择的语言
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        // 设置默认语言为中文
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }

    /**
     * 配置语言切换拦截器
     * <p>
     * 通过请求参数 lang 切换语言，例如：?lang=zh_CN 或 ?lang=en
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        // 设置请求参数名
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
