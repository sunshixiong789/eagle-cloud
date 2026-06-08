package com.eagle.payment.core.domain.repository;

import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Transfer 聚合根 Repository。
 *
 * @author sunshixiong
 */
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByBizTransferNo(String bizTransferNo);

    Optional<Transfer> findByChannelAndChannelTransferNo(PaymentChannel channel,
                                                        String channelTransferNo);

    boolean existsByBizTransferNo(String bizTransferNo);

    /**
     * 加 PESSIMISTIC_WRITE 锁查询,用于审核操作防并发双审。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transfer t WHERE t.id = :id")
    Optional<Transfer> findByIdForUpdate(@Param("id") Long id);

    /**
     * 当日累计提现金额 (按状态 IN (SUBMITTED, SUCCESS) 汇总)。
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
            "WHERE t.status IN :statuses " +
            "AND t.createTime >= :start AND t.createTime < :end")
    BigDecimal sumAmountInPeriod(@Param("statuses") List<TransferStatus> statuses,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 当日提现笔数 (按状态汇总)。
     */
    @Query("SELECT COUNT(t) FROM Transfer t " +
            "WHERE t.status IN :statuses " +
            "AND t.createTime >= :start AND t.createTime < :end")
    long countInPeriod(@Param("statuses") List<TransferStatus> statuses,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);
}
