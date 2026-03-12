package com.eagle.common.exception;

import com.eagle.common.i18n.MessageSourceUtil;
import lombok.Getter;

/**
 * 错误码枚举
 * <p>
 * 定义系统中常用的业务错误码，统一管理错误码和错误消息。
 * 按照阿里规范，错误码设计应该具有可读性，能快速定位问题。
 * <p>
 * <strong>国际化支持：</strong>
 * 每个错误码都有对应的消息键（messageKey），通过国际化资源文件获取实际消息。
 * 资源文件位置：
 * <ul>
 *   <li>中文：src/main/resources/messages_zh_CN.properties</li>
 *   <li>英文：src/main/resources/messages_en.properties</li>
 * </ul>
 *
 * <p>错误码规范：
 * <ul>
 *   <li>2xx: 成功状态</li>
 *   <li>4xx: 客户端错误</li>
 *   <li>5xx: 服务端错误</li>
 *   <li>自定义业务错误码: 1xxxx（5位数字）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用国际化消息
 * throw ErrorCode.USER_NOT_FOUND.toException();
 *
 * // 带上下文数据
 * throw ErrorCode.INVALID_PARAMETER.toException().putData("field", "username");
 *
 * // 使用自定义消息（忽略国际化）
 * throw ErrorCode.USER_NOT_FOUND.toException("用户ID: 123 不存在");
 *
 * // 带参数的国际化消息
 * throw ErrorCode.USER_NOT_FOUND.toExceptionWithArgs("张三");
 * }</pre>
 *
 * @author 孙士雄（sunshix@seeyon.com）
 */
@Getter
public enum ErrorCode {

    // ========== 通用错误码 4xx ==========
    /**
     * 请求参数错误
     */
    INVALID_PARAMETER(400, "error.common.invalid_parameter", "请求参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "error.common.unauthorized", "未授权，请先登录"),

    /**
     * 无权限
     */
    FORBIDDEN(403, "error.common.forbidden", "无权限访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "error.common.not_found", "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "error.common.method_not_allowed", "请求方法不支持"),

    /**
     * 请求冲突
     */
    CONFLICT(409, "error.common.conflict", "请求冲突"),

    /**
     * 请求频率过高
     */
    TOO_MANY_REQUESTS(429, "error.common.too_many_requests", "请求过于频繁，请稍后再试"),

    // ========== 服务端错误码 5xxx ==========
    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(500, "error.server.internal_error", "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "error.server.service_unavailable", "服务暂时不可用"),

    // ========== 用户相关错误码 10xxx ==========
    /**
     * 用户不存在
     */
    USER_NOT_FOUND(10001, "error.user.not_found", "用户不存在"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(10002, "error.user.already_exists", "用户已存在"),

    /**
     * 用户名或密码错误
     */
    INVALID_CREDENTIALS(10003, "error.user.invalid_credentials", "用户名或密码错误"),

    /**
     * 用户已被锁定
     */
    USER_LOCKED(10004, "error.user.locked", "用户已被锁定"),

    /**
     * 用户已被禁用
     */
    USER_DISABLED(10005, "error.user.disabled", "用户已被禁用"),

    /**
     * 密码强度不足
     */
    WEAK_PASSWORD(10006, "error.user.weak_password", "密码强度不足"),

    // ========== 认证相关错误码 11xxx ==========
    /**
     * Token无效
     */
    INVALID_TOKEN(11001, "error.auth.invalid_token", "Token无效"),

    /**
     * Token已过期
     */
    TOKEN_EXPIRED(11002, "error.auth.token_expired", "Token已过期"),

    /**
     * 验证码错误
     */
    INVALID_CAPTCHA(11003, "error.auth.invalid_captcha", "验证码错误"),

    /**
     * 验证码已过期
     */
    CAPTCHA_EXPIRED(11004, "error.auth.captcha_expired", "验证码已过期"),

    // ========== 数据验证错误码 12xxx ==========
    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(12001, "error.data.already_exists", "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND(12002, "error.data.not_found", "数据不存在"),

    /**
     * 数据格式错误
     */
    INVALID_DATA_FORMAT(12003, "error.data.invalid_format", "数据格式错误"),

    /**
     * 数据校验失败
     */
    DATA_VALIDATION_FAILED(12004, "error.data.validation_failed", "数据校验失败"),

    // ========== 业务操作错误码 13xxx ==========
    /**
     * 操作失败
     */
    OPERATION_FAILED(13001, "error.operation.failed", "操作失败"),

    /**
     * 重复操作
     */
    DUPLICATE_OPERATION(13002, "error.operation.duplicate", "请勿重复操作"),

    /**
     * 操作不允许
     */
    OPERATION_NOT_ALLOWED(13003, "error.operation.not_allowed", "当前状态不允许此操作"),

    /**
     * 依赖数据存在
     */
    DEPENDENT_DATA_EXISTS(13004, "error.operation.dependent_data_exists", "存在关联数据，无法删除"),

    // ========== 文件相关错误码 14xxx ==========
    /**
     * 文件不存在
     */
    FILE_NOT_FOUND(14001, "error.file.not_found", "文件不存在"),

    /**
     * 文件格式不支持
     */
    UNSUPPORTED_FILE_FORMAT(14002, "error.file.unsupported_format", "文件格式不支持"),

    /**
     * 文件大小超限
     */
    FILE_SIZE_EXCEEDED(14003, "error.file.size_exceeded", "文件大小超过限制"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(14004, "error.file.upload_failed", "文件上传失败"),

    // ========== 外部服务错误码 15xxx ==========
    /**
     * 外部服务调用失败
     */
    EXTERNAL_SERVICE_ERROR(15001, "error.external.service_error", "外部服务调用失败"),

    /**
     * 外部服务超时
     */
    EXTERNAL_SERVICE_TIMEOUT(15002, "error.external.service_timeout", "外部服务调用超时");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 国际化消息键
     */
    private final String messageKey;

    /**
     * 默认错误消息（用于国际化失败时的降级）
     */
    private final String defaultMessage;

    ErrorCode(Integer code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取国际化消息
     * <p>
     * 根据当前语言环境自动获取对应的消息
     *
     * @return 国际化消息
     */
    public String getMessage() {
        return MessageSourceUtil.getMessage(this.messageKey, null, this.defaultMessage);
    }

    /**
     * 获取国际化消息（带参数）
     * <p>
     * 支持参数化消息，例如："用户 {0} 不存在"
     *
     * @param args 消息参数
     * @return 国际化消息
     */
    public String getMessage(Object... args) {
        return MessageSourceUtil.getMessage(this.messageKey, args, this.defaultMessage);
    }

    /**
     * 转换为业务异常
     * <p>
     * 使用国际化消息
     *
     * @return BusinessException 实例
     */
    public BusinessException toException() {
        return new BusinessException(this.code, getMessage());
    }

    /**
     * 转换为业务异常（带参数的国际化消息）
     * <p>
     * 例如：ErrorCode.USER_NOT_FOUND.toExceptionWithArgs("张三")
     *
     * @param args 消息参数
     * @return BusinessException 实例
     */
    public BusinessException toExceptionWithArgs(Object... args) {
        return new BusinessException(this.code, getMessage(args));
    }

    /**
     * 转换为业务异常（带自定义消息）
     * <p>
     * 忽略国际化，使用自定义消息
     *
     * @param customMessage 自定义消息
     * @return BusinessException 实例
     */
    public BusinessException toException(String customMessage) {
        return new BusinessException(this.code, customMessage);
    }

    /**
     * 转换为业务异常（带原因异常）
     * <p>
     * 使用国际化消息，并携带原因异常
     *
     * @param cause 原因异常
     * @return BusinessException 实例
     */
    public BusinessException toException(Throwable cause) {
        return new BusinessException(this.code, getMessage(), cause);
    }
}
