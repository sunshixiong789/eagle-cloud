package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.mapper.TransferMapper;
import com.eagle.payment.core.application.service.TransferApplicationService;
import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.interfaces.dto.request.ApproveTransferRequest;
import com.eagle.payment.core.interfaces.dto.request.RejectTransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TransferAdminController 单元测试。
 *
 * <p>聚焦验证 JWT subject 透传 + 入参绑定 + service 调用契约。{@code @PreAuthorize}
 * 权限拦截 + {@code @Valid} 校验由 Spring 框架本身保证,这里跳过 MockMvc 集成测试
 * (避免与 OAuth2 Resource Server filter chain / Jackson 3 / @WebMvcTest 切片配置纠缠),
 * 直接对 controller 方法做单元断言。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransferAdminController")
class TransferAdminControllerTest {

    @Mock
    private TransferApplicationService transferApplicationService;
    @Mock
    private TransferMapper mapper;

    private TransferAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new TransferAdminController(transferApplicationService, mapper);
    }

    private Jwt jwtFor(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private Transfer sample() {
        return Transfer.create("TRN-001", TransferMode.APPROVAL, PaymentChannel.ALIPAY,
                "user@example.com", "张三", new BigDecimal("500.00"), "结算");
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("应将 JWT subject 作为 approverId + remark 转发到 service.approve")
        void shouldForwardJwtSubjectAndRemark() {
            Transfer t = sample();
            when(transferApplicationService.approve(eq(1L), eq("admin-1"), eq("ok")))
                    .thenReturn(t);
            when(mapper.toResponse(any(Transfer.class))).thenReturn(null);
            ApproveTransferRequest req = new ApproveTransferRequest();
            req.setRemark("ok");

            controller.approve(1L, req, jwtFor("admin-1"));

            verify(transferApplicationService).approve(1L, "admin-1", "ok");
            verify(mapper).toResponse(t);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("应将 JWT subject + reason 转发到 service.reject")
        void shouldForwardJwtSubjectAndReason() {
            Transfer t = sample();
            when(transferApplicationService.reject(eq(1L), eq("admin-1"), eq("金额可疑")))
                    .thenReturn(t);
            when(mapper.toResponse(any(Transfer.class))).thenReturn(null);
            RejectTransferRequest req = new RejectTransferRequest();
            req.setReason("金额可疑");

            controller.reject(1L, req, jwtFor("admin-1"));

            verify(transferApplicationService).reject(1L, "admin-1", "金额可疑");
            verify(mapper).toResponse(t);
        }
    }
}
