package com.eagle.payment.core.domain.repository;

import com.eagle.payment.core.domain.model.aggregate.ReconcileDiff;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账差异 Repository。
 *
 * @author sunshixiong
 */
public interface ReconcileDiffRepository extends JpaRepository<ReconcileDiff, Long> {

    List<ReconcileDiff> findByBillDateAndChannel(LocalDate billDate, PaymentChannel channel);

    List<ReconcileDiff> findByResolvedFalseOrderByCreateTimeDesc();
}
