package com.eagle.common.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类
 * <p>
 * 用于获取国际化消息，支持多语言环境。
 * 按照阿里规范，国际化应该统一管理，便于维护和扩展。
 *
 * @author 孙士雄（sunshix@seeyon.com）
 */
@Slf4j
@Component
public class MessageSourceUtil {

    private static MessageSource messageSource;

    public MessageSourceUtil(MessageSource messageSource) {
        MessageSourceUtil.messageSource = messageSource;
    }

    /**
     * 获取国际化消息
     *
     * @param code 消息键
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        return getMessage(code, null);
    }

    /**
     * 获取国际化消息（带参数）
     *
     * @param code 消息键
     * @param args 消息参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args) {
        return getMessage(code, args, "");
    }

    /**
     * 获取国际化消息（带参数和默认消息）
     *
     * @param code           消息键
     * @param args           消息参数
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 获取国际化消息（指定语言环境）
     *
     * @param code           消息键
     * @param args           消息参数
     * @param defaultMessage 默认消息
     * @param locale         语言环境
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        try {
            if (messageSource == null) {
                log.warn("MessageSource未初始化，返回默认消息: {}", defaultMessage);
                return defaultMessage;
            }
            return messageSource.getMessage(code, args, defaultMessage, locale);
        } catch (Exception e) {
            log.warn("获取国际化消息失败，code: {}, 返回默认消息: {}", code, defaultMessage, e);
            return defaultMessage;
        }
    }

    /**
     * 获取当前语言环境
     *
     * @return 语言环境
     */
    public static Locale getLocale() {
        return LocaleContextHolder.getLocale();
    }
}
