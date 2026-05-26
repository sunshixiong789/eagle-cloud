package com.eagle.auth.core.domain.model.valueobject;

import com.eagle.auth.core.domain.model.enums.FreezeReason;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号冻结信息值对象
 * <p>
 * 当 Account.status = FROZEN 时此值对象的字段为非 null。
 *
 * @author sunshixiong
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountFreeze {

    @Enumerated(EnumType.STRING)
    @Column(name = "freeze_reason", length = 20, comment = "冻结原因")
    private FreezeReason reason;

    @Column(name = "freeze_until", comment = "冻结到期时间（null=永久）")
    private LocalDateTime freezeUntil;

    @Column(name = "frozen_by", comment = "冻结操作人ID")
    private Long operatorId;

    @Column(name = "frozen_by_name", length = 64, comment = "冻结操作人姓名")
    private String operatorName;

    @Column(name = "freeze_remark", length = 255, comment = "冻结备注")
    private String remark;

    @Column(name = "frozen_at", comment = "冻结时间")
    private LocalDateTime frozenAt;

    /**
     * 判断当前冻结是否已到期。永久冻结永远返回 false
     */
    public boolean isExpired(LocalDateTime now) {
        return freezeUntil != null && now.isAfter(freezeUntil);
    }
}
