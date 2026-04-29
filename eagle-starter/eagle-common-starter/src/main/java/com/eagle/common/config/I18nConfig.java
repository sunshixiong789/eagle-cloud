package com.eagle.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * 国际化配置。
 *
 * <p>使用 {@link AcceptHeaderLocaleResolver}，从请求头 {@code Accept-Language} 中解析语言环境。
 * 无状态、无 Session 依赖，适合 REST API 和无状态微服务架构。
 *
 * <p>支持中文（简体/繁体）和英语，未匹配时默认使用简体中文。
 *
 * @author 孙士雄
 */
@Configuration
public class I18nConfig {

    /**
     * 无状态 Accept-Language 语言解析器。
     *
     * <p>客户端通过请求头 {@code Accept-Language: zh-CN} 或 {@code en-US} 切换语言。
     * 未携带或未匹配时，回退到 {@link Locale#SIMPLIFIED_CHINESE}。
     */
    @Bean
    @ConditionalOnMissingBean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        localeResolver.setSupportedLocales(List.of(
                Locale.SIMPLIFIED_CHINESE,
                Locale.TRADITIONAL_CHINESE,
                Locale.ENGLISH
        ));
        return localeResolver;
    }
}
