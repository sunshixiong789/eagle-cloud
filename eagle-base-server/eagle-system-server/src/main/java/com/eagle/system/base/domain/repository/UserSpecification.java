package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> usernameLike(String username) {
        return (root, query, cb) -> username == null ? null : cb.like(root.get("username"), "%" + username + "%");
    }

    public static Specification<User> deptIdEquals(Long deptId) {
        return (root, query, cb) -> deptId == null ? null : cb.equal(root.get("deptId"), deptId);
    }

    public static Specification<User> lockFlagEquals(Boolean lockFlag) {
        return (root, query, cb) -> lockFlag == null ? null : cb.equal(root.get("lockFlag"), lockFlag);
    }

    public static Specification<User> phoneLike(String phone) {
        return (root, query, cb) -> phone == null ? null : cb.like(root.get("phone"), "%" + phone + "%");
    }

    public static Specification<User> emailLike(String email) {
        return (root, query, cb) -> email == null ? null : cb.like(root.get("email"), "%" + email + "%");
    }
}
