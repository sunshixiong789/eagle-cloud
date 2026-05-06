package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * 用户查询规格（Specification Pattern）
 */
public class UserSpecification {

    public static Specification<User> usernameLike(String username) {
        return (root, query, cb) -> username == null ? null : cb.like(root.get("username"), "%" + username + "%");
    }

    public static Specification<User> emailLike(String email) {
        return (root, query, cb) -> email == null ? null : cb.like(root.get("email"), "%" + email + "%");
    }
}
