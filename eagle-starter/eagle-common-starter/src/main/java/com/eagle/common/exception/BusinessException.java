package com.eagle.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 用于业务逻辑中的异常场景，支持错误码、异常链、上下文数据等特性。
 * 按照阿里规范，业务异常应该继承 RuntimeException，避免强制捕获。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 简单使用
 * throw new BusinessException(400, "用户不存在");
 *
 * // 携带原因异常
 * throw new BusinessException(500, "数据库操作失败", sqlException);
 *
 * // 携带上下文数据
 * throw BusinessException.of(404, "订单不存在")
 *     .putData("orderId", orderId)
 *     .putData("userId", userId);
 * }</pre>
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/8-10:23
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 构造业务异常
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（带原因）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 构造业务异常（带格式化消息）
     *
     * @param code            错误码
     * @param messageTemplate 消息模板，支持 String.format 格式
     * @param args            消息参数
     */
    public BusinessException(Integer code, String messageTemplate, Object... args) {
        super(String.format(messageTemplate, args));
        this.code = code;
    }

    /**
     * 静态工厂方法
     *
     * @param code    错误码
     * @param message 错误消息
     * @return BusinessException 实例
     */
    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 静态工厂方法（带原因）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param cause   原始异常
     * @return BusinessException 实例
     */
    public static BusinessException of(Integer code, String message, Throwable cause) {
        return new BusinessException(code, message, cause);
    }


}
