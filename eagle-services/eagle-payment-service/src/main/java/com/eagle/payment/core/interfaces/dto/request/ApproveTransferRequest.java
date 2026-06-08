package com.eagle.payment.core.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * Admin 审核通过请求。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "审核通过请求")
public class ApproveTransferRequest {

    @Size(max = 512)
    @Schema(description = "审核备注 (可选)", example = "金额合规已核对")
    @Nullable
    private String remark;
}
