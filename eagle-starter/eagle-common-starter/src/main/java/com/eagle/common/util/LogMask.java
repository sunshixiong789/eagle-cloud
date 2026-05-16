package com.eagle.common.util;

/**
 * 日志敏感字段脱敏工具。
 *
 * <p>仅用于日志输出，不用于响应/存储。规则参考 13-logging.md：</p>
 * <ul>
 *   <li>手机号：保留首 3 + 尾 4，如 {@code 138****1234}</li>
 *   <li>邮箱：邮箱本地部分仅首字母，如 {@code a***@example.com}</li>
 *   <li>身份证：保留首 3 + 尾 4，中间星号，如 {@code 110***********1234}</li>
 *   <li>Token：仅打印前 8 位 + {@code ***}</li>
 * </ul>
 *
 * <p>输入为 {@code null} 或太短时返回 {@code ***}，避免脱敏算法本身泄漏长度信息。</p>
 *
 * @author sunshixiong
 */
public final class LogMask {

    private static final String MASKED = "***";

    private LogMask() {
    }

    /** 手机号脱敏：13800001234 → 138****1234 */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return MASKED;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏：alice@example.com → a***@example.com */
    public static String email(String email) {
        if (email == null) {
            return MASKED;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return MASKED;
        }
        return email.charAt(0) + MASKED + email.substring(at);
    }

    /** 身份证脱敏：110101199001011234 → 110***********1234 */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return MASKED;
        }
        return idCard.substring(0, 3) + "*".repeat(idCard.length() - 7) + idCard.substring(idCard.length() - 4);
    }

    /** Token 脱敏：仅保留前 8 位 + *** */
    public static String token(String token) {
        if (token == null || token.length() < 8) {
            return MASKED;
        }
        return token.substring(0, 8) + MASKED;
    }
}
