package com.eagle.payment.core.domain.repository;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment 聚合根 Repository。
 *
 * @author sunshixiong
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 按业务订单号 + 渠道查找;用于幂等判断。
     */
    Optional<Payment> findByBizOrderNoAndChannel(String bizOrderNo, PaymentChannel channel);

    /**
     * 按渠道交易号查找;用于异步回调匹配。
     */
    Optional<Payment> findByChannelAndOutTradeNo(PaymentChannel channel, String outTradeNo);

    /**
     * 查询过期未处理订单 (定时任务批量过期使用)。
     */
    List<Payment> findByStatusInAndExpiresAtBefore(List<PaymentStatus> statuses,
                                                   LocalDateTime threshold);

    /**
     * 判断业务订单号是否已存在 (Mode A 幂等冲突匹配)。
     */
    boolean existsByBizOrderNoAndChannel(String bizOrderNo, PaymentChannel channel);
}
