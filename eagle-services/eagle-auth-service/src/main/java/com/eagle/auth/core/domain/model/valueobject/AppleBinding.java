package com.eagle.auth.core.domain.model.valueobject;

import com.eagle.auth.core.domain.AuthErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Apple 登录绑定信息。
 *
 * <p>{@code subject} 来自服务端验签后的 Apple identity token {@code sub}，
 * 绝不信任客户端单独提交的 user 标识。
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppleBinding {

    @Column(name = "apple_subject", length = 255, unique = true,
            comment = "Apple Sign In subject")
    private String subject;

    @Column(name = "apple_bind_time", comment = "Apple 绑定时间")
    private LocalDateTime bindTime;

    private AppleBinding(String subject, LocalDateTime bindTime) {
        this.subject = subject;
        this.bindTime = bindTime;
    }

    public static AppleBinding create(String subject) {
        if (subject == null || subject.isBlank()) {
            throw AuthErrorCode.APPLE_SUBJECT_REQUIRED.toDomainException();
        }
        return new AppleBinding(subject, LocalDateTime.now());
    }
}
