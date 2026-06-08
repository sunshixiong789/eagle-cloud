package com.eagle.payment.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin 审核拒绝请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "审核拒绝请求")
public class RejectTransferRequest {

    @NotBlank
    @Size(max = 512)
    @Schema(description = "拒绝原因",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "金额可疑,需补充资料")
    private String reason;
}
