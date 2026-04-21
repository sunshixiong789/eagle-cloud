package com.eagle.system.upms.domain.repository;

import com.eagle.system.upms.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * 用户查询规格（Specification Pattern）
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
 * Specification&lt;User&gt; spec = Specification
 *     .where(UserSpecification.usernameLike("admin"))
 *     .and(UserSpecification.deptIdEquals(1L));
 * List&lt;User&gt; users = userRepository.findAll(spec);
 * </pre>
 * <p>
 * 优势：
 * <ul>
 *   <li>避免字符串拼接 SQL，防止 SQL 注入</li>
 *   <li>编译期类型检查，减少运行时错误</li>
 *   <li>可复用的查询条件，易于维护</li>
 * </ul>
 */
public class UserSpecification {

    /**
     * 用户名模糊查询
     * <p>
     * 使用 LIKE '%username%' 进行模糊匹配
     *
     * @param username 用户名（null 时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<User> usernameLike(String username) {
        return (root, query, cb) -> username == null ? null : cb.like(root.get("username"), "%" + username + "%");
    }

    /**
     * 部门 ID 精确查询
     *
     * @param deptId 部门 ID（null 时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<User> deptIdEquals(Long deptId) {
        return (root, query, cb) -> deptId == null ? null : cb.equal(root.get("deptId"), deptId);
    }

    /**
     * 邮箱模糊查询
     * <p>
     * 使用 LIKE '%email%' 进行模糊匹配
     *
     * @param email 邮箱（null 时返回 null，该条件会被忽略）
     * @return JPA Specification
     */
    public static Specification<User> emailLike(String email) {
        return (root, query, cb) -> email == null ? null : cb.like(root.get("email"), "%" + email + "%");
    }
}
