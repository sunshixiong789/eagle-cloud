package com.eagle.common.exception;

import lombok.Getter;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * 应用异常基类
 * <p>
 * 所有业务异常均继承此类。持有 {@link ErrorCode} 引用，
 * 通过 {@link #getLocalizedMessage(MessageSource, Locale)} 按请求语言解析消息。
 * <p>
 * 子类按 HTTP 语义分类：
 * <ul>
 *   <li>{@link NotFoundException} — HTTP 404</li>
 *   <li>{@link ConflictException} — HTTP 409</li>
 *   <li>{@link DomainException} — HTTP 400（领域验证/状态不变性）</li>
 *   <li>{@link ServiceException} — HTTP 500（基础设施/外部服务故障）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Getter
public abstract class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * -- GETTER --
     *  获取错误码
     */
    private final transient ErrorCode errorCode;
    /**
     * -- GETTER --
     *  获取消息参数（供扩展使用）
     */
    private final transient Object[] messageArgs;

    protected AppException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    protected AppException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }

    /**
     * 按指定 Locale 解析 i18n 消息，默认消息作为降级
     *
     * @param messageSource Spring MessageSource
     * @param locale        当前请求语言环境
     * @return 解析后的消息文本
     */
    public String getLocalizedMessage(MessageSource messageSource, Locale locale) {
        return messageSource.getMessage(
                errorCode.getMessageKey(),
                messageArgs,
                errorCode.getDefaultMessage(),
                locale
        );
    }

}
