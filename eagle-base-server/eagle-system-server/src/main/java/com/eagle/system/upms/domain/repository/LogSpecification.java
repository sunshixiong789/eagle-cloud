package com.eagle.system.upms.domain.repository;

import com.eagle.system.upms.domain.model.SysLog;
import com.eagle.system.upms.domain.model.enums.LogStatus;
import com.eagle.system.upms.domain.model.enums.LogType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * 日志查询规格（Specification Pattern）
 * <p>
 * 职责：
 * <ul>
 *   <li>提供类型安全的动态查询条件构建</li>
 *   <li>支持条件组合（AND、OR）</li>
 *   <li>自动处理 null 值（null 条件会被忽略）</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 * Specification&lt;SysLog&gt; spec = Specification
 *     .where(LogSpecification.logTypeEquals("LOGIN"))
 *     .and(LogSpecification.statusEquals("SUCCESS"));
 * Page&lt;SysLog&gt; logs = logRepository.findAll(spec, pageable);
 * </pre>
 *
 * @author sunshixiong
 */
public class LogSpecification {

    /**
     * 日志类型精确查询
     * <p>
     * 将字符串转换为 {@link LogType} 枚举后进行精确匹配
     *
     * @param logType 日志类型字符串（null 或空白时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<SysLog> logTypeEquals(String logType) {
        return (root, query, cb) -> {
            if (logType == null || logType.isBlank()) {
                return null;
            }
            return cb.equal(root.get("logType"), LogType.valueOf(logType));
        };
    }

    /**
     * 日志状态精确查询
     * <p>
     * 将字符串转换为 {@link LogStatus} 枚举后进行精确匹配
     *
     * @param status 日志状态字符串（null 或空白时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<SysLog> statusEquals(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                return null;
            }
            return cb.equal(root.get("status"), LogStatus.valueOf(status));
        };
    }

    /**
     * 用户名模糊查询
     * <p>
     * 使用 LIKE '%username%' 进行模糊匹配
     *
     * @param username 用户名（null 或空白时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<SysLog> usernameLike(String username) {
        return (root, query, cb) -> {
            if (username == null || username.isBlank()) {
                return null;
            }
            return cb.like(root.get("username"), "%" + username + "%");
        };
    }

    /**
     * 请求 URI 模糊查询
     * <p>
     * 使用 LIKE '%requestUri%' 进行模糊匹配
     *
     * @param requestUri 请求 URI（null 或空白时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<SysLog> requestUriLike(String requestUri) {
        return (root, query, cb) -> {
            if (requestUri == null || requestUri.isBlank()) {
                return null;
            }
            return cb.like(root.get("requestUri"), "%" + requestUri + "%");
        };
    }

    /**
     * 请求 IP 模糊查询
     * <p>
     * 使用 LIKE '%remoteAddr%' 进行模糊匹配
     *
     * @param remoteAddr 请求 IP（null 或空白时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<SysLog> remoteAddrLike(String remoteAddr) {
        return (root, query, cb) -> {
            if (remoteAddr == null || remoteAddr.isBlank()) {
                return null;
            }
            return cb.like(root.get("remoteAddr"), "%" + remoteAddr + "%");
        };
    }

    /**
     * 创建时间范围查询
     * <p>
     * 查询创建时间在 start 和 end 之间的日志记录
     *
     * @param start 开始时间（null 时忽略下界）
     * @param end   结束时间（null 时忽略上界）
     * @return JPA Specification
     */
    public static Specification<SysLog> createTimeBetween(LocalDateTime start,
                                                          LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }
            if (start != null && end != null) {
                return cb.between(root.get("createTime"), start, end);
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("createTime"), start);
            }
            return cb.lessThanOrEqualTo(root.get("createTime"), end);
        };
    }
}
