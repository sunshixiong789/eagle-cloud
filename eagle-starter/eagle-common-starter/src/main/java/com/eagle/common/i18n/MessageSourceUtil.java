package com.eagle.common.i18n;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * 国际化消息静态工具类。
 *
 * <p>供无法注入 {@link MessageSource} 的场景（静态工具、枚举）使用。
 * 需要在 Spring 容器启动后通过 {@link #init(MessageSource)} 完成初始化，
 * 由 {@link com.eagle.common.config.EagleCommonAutoConfiguration} 自动调用，无需手动配置。
 *
 * <p><b>优先使用注入方式：</b>在 Spring Bean 中，优先直接注入 {@link MessageSource} 并调用
 * {@code messageSource.getMessage(code, args, defaultMessage, locale)}，
 * 只在确实无法注入的场景才使用本工具类。
 *
 * @author 孙士雄
 */
@Slf4j
public final class MessageSourceUtil {

    private static volatile MessageSource messageSource;

    private MessageSourceUtil() {
    }

    /**
     * 由自动配置在容器启动时调用，完成 MessageSource 初始化。
     *
     * @param source Spring 容器中的 MessageSource Bean
     */
    public static void init(MessageSource source) {
        MessageSourceUtil.messageSource = source;
    }

    /**
     * 获取国际化消息（使用当前线程 Locale）。
     *
     * @param code 消息键
     * @return 国际化消息，未找到时返回空字符串
     */
    public static String getMessage(String code) {
        return getMessage(code, null, "");
    }

    /**
     * 获取国际化消息（带参数，使用当前线程 Locale）。
     *
     * @param code 消息键
     * @param args 消息参数（对应消息模板中的 {0}、{1} 占位符）
     * @return 国际化消息，未找到时返回空字符串
     */
    public static String getMessage(String code, Object[] args) {
        return getMessage(code, args, "");
    }

    /**
     * 获取国际化消息（带参数和默认消息，使用当前线程 Locale）。
     *
     * @param code           消息键
     * @param args           消息参数
     * @param defaultMessage key 不存在时的降级消息
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 获取国际化消息（指定 Locale）。
     *
     * @param code           消息键
     * @param args           消息参数
     * @param defaultMessage key 不存在时的降级消息
     * @param locale         语言环境
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        if (messageSource == null) {
            log.warn("MessageSourceUtil not initialized, returning default message for key: {}", code);
            return defaultMessage;
        }
        try {
            return messageSource.getMessage(code, args, defaultMessage, locale);
        } catch (Exception e) {
            log.warn("Failed to resolve i18n message for key: {}, returning default", code, e);
            return defaultMessage;
        }
    }
}