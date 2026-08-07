package com.eagle.common.exception;

import com.eagle.common.i18n.MessageSourceUtil;

import java.util.Locale;

/**
 * 错误码接口
 *
 * <p>各业务域枚举实现此接口，<strong>只需覆写一个方法</strong> {@link #meta()}，
 * 其余三个 getter 均由接口的 default 方法委托给 {@link Meta} Record 提供。
 *
 * <p>用法示例：
 * <pre>{@code
 * throw UserErrorCode.USER_NOT_FOUND.toNotFoundException();
 * throw AuthErrorCode.SMS_RATE_LIMIT.toServiceException();
 * throw SystemErrorCode.DICT_NOT_FOUND.toNotFoundException();
 * }</pre>
 *
 * <p>新增错误码只需在对应枚举文件中增加一行常量，无需修改其他代码：
 * <pre>{@code
 * ORDER_ITEM_LIMIT_EXCEEDED(30005, "error.order.item_limit", "订单项超出上限");
 * }</pre>
 *
 * @author sunshixiong
 */
public interface ErrorCode {

    /**
     * 返回当前错误码的元数据，这是实现类<strong>唯一需要覆写</strong>的方法。
     *
     * <p>标准实现：
     * <pre>{@code
     * private final ErrorCode.Meta meta;
     *
     * MyErrorCode(int code, String messageKey, String defaultMessage) {
     *     this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
     * }
     *
     * @Override
     * public ErrorCode.Meta meta() { return meta; }
     * }</pre>
     */
    Meta meta();

    /**
     * 数字错误码，用于 API 响应和日志定位
     */
    default int getCode() {
        return meta().code();
    }

    // ==================== 消息解析（由 Meta 委托，实现类无需覆写）====================

    /**
     * i18n 资源键，对应 messages_*.properties 中的 key
     */
    default String getMessageKey() {
        return meta().messageKey();
    }

    /**
     * 当 i18n 解析失败时的中文降级消息
     */
    default String getDefaultMessage() {
        return meta().defaultMessage();
    }

    /**
     * 使用 LocaleContextHolder 解析当前语言消息（MVC 上下文）
     */
    default String getMessage() {
        return MessageSourceUtil.getMessage(getMessageKey(), null, getDefaultMessage());
    }

    /**
     * 带参数的消息解析，支持 {0} {1} 占位符
     *
     * @param args 消息参数
     */
    default String getMessage(Object... args) {
        return MessageSourceUtil.getMessage(getMessageKey(), args, getDefaultMessage());
    }

    /**
     * 显式传入 Locale（过滤器/异步上下文中使用，LocaleContextHolder 不可用时）
     *
     * @param locale 语言环境
     */
    default String getMessage(Locale locale) {
        return MessageSourceUtil.getMessage(getMessageKey(), null, getDefaultMessage(), locale);
    }

    /**
     * 创建 HTTP 404 Not Found 异常
     *
     * @param args 消息占位符参数（可选）
     */
    default NotFoundException toNotFoundException(Object... args) {
        return new NotFoundException(this, args);
    }

    // ==================== 异常工厂方法 ====================

    /**
     * 创建 HTTP 409 Conflict 异常
     *
     * @param args 消息占位符参数（可选）
     */
    default ConflictException toConflictException(Object... args) {
        return new ConflictException(this, args);
    }

    /**
     * 创建 HTTP 400 Bad Request 异常（领域验证、状态不变性）
     *
     * @param args 消息占位符参数（可选）
     */
    default DomainException toDomainException(Object... args) {
        return new DomainException(this, args);
    }

    /**
     * 创建 HTTP 403 Forbidden 异常（已认证但无权操作该资源，如访问他人数据）
     *
     * <p>注意与 {@link #toNotFoundException(Object...)} 的取舍：若「资源存在与否」本身敏感，
     * 应返回 404 避免存在性泄漏；若资源归属对调用方已知（如后台按 ID 操作），用本方法返回 403。
     *
     * @param args 消息占位符参数（可选）
     */
    default ForbiddenException toForbiddenException(Object... args) {
        return new ForbiddenException(this, args);
    }

    /**
     * 创建 HTTP 500 Internal Server Error 异常（基础设施、外部服务故障）
     *
     * @param args 消息占位符参数（可选）
     */
    default ServiceException toServiceException(Object... args) {
        return new ServiceException(this, args);
    }

    /**
     * 创建 HTTP 500 Internal Server Error 异常（带原因异常链）
     *
     * @param cause 原始异常
     */
    default ServiceException toServiceException(Throwable cause) {
        return new ServiceException(this, cause);
    }

    /**
     * 聚合错误码全部元数据的 Record，由枚举构造器包装后通过 {@link #meta()} 返回。
     * 实现类只需持有一个此字段即可，无需再单独声明 code / messageKey / defaultMessage 三个字段。
     *
     * @param code           数字错误码，用于 API 响应和日志定位
     * @param messageKey     i18n 资源键，对应 messages_*.properties 中的 key
     * @param defaultMessage 当 i18n 解析失败时的中文降级消息
     */
    record Meta(int code, String messageKey, String defaultMessage) {
    }
}
