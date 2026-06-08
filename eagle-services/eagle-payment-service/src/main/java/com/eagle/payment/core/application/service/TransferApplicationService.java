package com.eagle.payment.core.application.service;

import com.eagle.payment.core.common.exception.TransferErrorCode;
import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import com.eagle.payment.core.domain.port.GatewayTransferCommand;
import com.eagle.payment.core.domain.port.GatewayTransferResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.TransferRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreateTransferRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Transfer 应用服务: 用例编排 + 风控 + 事务边界。
 *
 * <p>风控规则 (按 eagle.payment.transfer.*):
 * <ol>
 *   <li>提现总开关 - {@code enabled=false} 直接拒</li>
 *   <li>单笔限额 - amount &gt; single-amount-limit 拒</li>
 *   <li>当日累计金额限额 - 当日 SUBMITTED + SUCCESS 的金额 + 新单 &gt; daily-amount-limit 拒</li>
 *   <li>当日笔数限额 - 当日 SUBMITTED + SUCCESS 的笔数 + 1 &gt; daily-count-limit 拒</li>
 * </ol>
 *
 * <p>风控通过后:
 * <ol>
 *   <li>幂等检查 (bizTransferNo) UNIQUE,DB 兜底</li>
 *   <li>创建 Transfer (PENDING) → 提交到渠道</li>
 *   <li>支付宝同步 SUCCESS → 直接 markSucceeded;微信异步 SUBMITTED → submittedToChannel 等回调</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class TransferApplicationService {

    private static final List<TransferStatus> ACCOUNTED_STATUSES =
            List.of(TransferStatus.SUBMITTED, TransferStatus.SUCCESS);

    private final TransferRepository transferRepository;
    private final PaymentProperties properties;
    private final Map<PaymentChannel, PaymentGatewayPort> gateways;

    public TransferApplicationService(TransferRepository transferRepository,
                                      PaymentProperties properties,
                                      List<PaymentGatewayPort> gatewayPorts) {
        this.transferRepository = transferRepository;
        this.properties = properties;
        Map<PaymentChannel, PaymentGatewayPort> map = new EnumMap<>(PaymentChannel.class);
        for (PaymentGatewayPort port : gatewayPorts) {
            map.put(port.getChannel(), port);
        }
        this.gateways = map;
    }

    @Transactional
    public Transfer create(CreateTransferRequest request) {
        if (!properties.getTransfer().isEnabled()) {
            throw TransferErrorCode.TRANSFER_DISABLED.toDomainException();
        }
        checkRiskControl(request.getAmount());

        if (transferRepository.existsByBizTransferNo(request.getBizTransferNo())) {
            throw TransferErrorCode.DUPLICATE_TRANSFER.toConflictException();
        }
        PaymentGatewayPort gateway = gateways.get(request.getChannel());
        if (gateway == null) {
            throw TransferErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        Transfer transfer = Transfer.create(request.getBizTransferNo(),
                request.getMode(),
                request.getChannel(), request.getRecipientAccount(),
                request.getRecipientName(), request.getAmount(), request.getReason());
        try {
            transfer = transferRepository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException e) {
            if (transferRepository.existsByBizTransferNo(request.getBizTransferNo())) {
                throw TransferErrorCode.DUPLICATE_TRANSFER.toConflictException();
            }
            throw e;
        }

        if (request.getMode() == TransferMode.APPROVAL) {
            log.info("transfer created (awaiting approval), id={}, channel={}, status={}",
                    transfer.getId(), request.getChannel(), transfer.getStatus());
            return transfer;
        }
        return submitToGateway(transfer, gateway);
    }

    /**
     * 把 PENDING 状态 transfer 推送到渠道,并按渠道返回结果更新状态。
     * 复用于 IMMEDIATE 模式 create() 和 APPROVAL 模式 approve() 两个入口。
     */
    private Transfer submitToGateway(Transfer transfer, PaymentGatewayPort gateway) {
        GatewayTransferResult result = gateway.transfer(new GatewayTransferCommand(
                transfer.getChannel(),
                transfer.getBizTransferNo(),
                transfer.getAmount(),
                "CNY",
                transfer.getRecipientAccount(),
                transfer.getRecipientName(),
                transfer.getReason()
        ));
        if (result.status() == TransferStatus.SUCCESS) {
            transfer.submittedToChannel(result.channelTransferNo());
            transfer.markSucceeded(
                    result.succeededAt() != null ? result.succeededAt() : LocalDateTime.now(),
                    result.channelTransferNo());
        } else if (result.status() == TransferStatus.FAILED) {
            transfer.markFailed(result.failReason());
        } else {
            transfer.submittedToChannel(result.channelTransferNo());
        }
        Transfer saved = transferRepository.save(transfer);
        log.info("transfer submitted to gateway, id={}, channel={}, status={}, channelTransferNo={}",
                saved.getId(), saved.getChannel(), saved.getStatus(),
                saved.getChannelTransferNo());
        return saved;
    }

    /**
     * 审核通过: 仅允许 APPROVAL 模式 PENDING_APPROVAL 状态迁出。
     * 同事务调渠道,与 IMMEDIATE create 流程行为对称。
     */
    @Transactional
    public Transfer approve(Long transferId, String approverId, @Nullable String remark) {
        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
        transfer.approve(approverId);
        PaymentGatewayPort gateway = gateways.get(transfer.getChannel());
        if (gateway == null) {
            throw TransferErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        Transfer result = submitToGateway(transfer, gateway);
        log.info("transfer approved, id={}, approverId={}, remark={}, finalStatus={}",
                result.getId(), approverId, remark, result.getStatus());
        return result;
    }

    /**
     * 审核拒绝: 仅允许 PENDING_APPROVAL 迁出 → REJECTED 终态,不调渠道。
     */
    @Transactional
    public Transfer reject(Long transferId, String approverId, String reason) {
        Transfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
        transfer.reject(approverId, reason);
        Transfer saved = transferRepository.save(transfer);
        log.info("transfer rejected, id={}, approverId={}, reason={}",
                saved.getId(), approverId, reason);
        return saved;
    }

    @Transactional(readOnly = true)
    public Transfer findById(Long transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
    }

    @Transactional(readOnly = true)
    public Transfer findByBizTransferNo(String bizTransferNo) {
        return transferRepository.findByBizTransferNo(bizTransferNo)
                .orElseThrow(TransferErrorCode.TRANSFER_NOT_FOUND::toNotFoundException);
    }

    /**
     * 风控:单笔限额 + 当日累计金额 / 笔数限额。
     */
    private void checkRiskControl(BigDecimal amount) {
        PaymentProperties.Transfer cfg = properties.getTransfer();
        if (amount.compareTo(BigDecimal.valueOf(cfg.getSingleAmountLimit())) > 0) {
            throw TransferErrorCode.EXCEED_SINGLE_LIMIT.toDomainException();
        }
        LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime end = start.plusDays(1);
        BigDecimal todayAmount = transferRepository.sumAmountInPeriod(
                ACCOUNTED_STATUSES, start, end);
        if (todayAmount.add(amount).compareTo(
                BigDecimal.valueOf(cfg.getDailyAmountLimit())) > 0) {
            throw TransferErrorCode.EXCEED_DAILY_AMOUNT.toDomainException();
        }
        long todayCount = transferRepository.countInPeriod(
                ACCOUNTED_STATUSES, start, end);
        if (todayCount + 1 > cfg.getDailyCountLimit()) {
            throw TransferErrorCode.EXCEED_DAILY_COUNT.toDomainException();
        }
    }
}
