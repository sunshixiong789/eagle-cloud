package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSpecificationTest {

    @Test
    @DisplayName("姓名模糊查询应匹配档案姓名")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldMatchProfileNameWhenNameProvided() {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path profilePath = mock(Path.class);
        Path<String> namePath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("profile")).thenReturn(profilePath);
        when(profilePath.get("name")).thenReturn(namePath);
        when(cb.like(namePath, "%Alice Real%")).thenReturn(predicate);

        UserSpecification.nameLike("Alice Real").toPredicate(root, query, cb);

        verify(cb).like(namePath, "%Alice Real%");
    }
}
