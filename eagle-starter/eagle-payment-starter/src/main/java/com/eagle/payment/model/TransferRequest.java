package com.eagle.payment.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一企业付款/转账请求。
 *
 * <p>用于提现、红包、退款补偿等将资金转账给用户的场景。
 * 支付宝通过 {@code alipay.fund.trans.uni.transfer} 接口实现；
 * 微信支付通过企业付款到零钱接口实现。
 *
 * <p>注意：
 * <ul>
 *   <li>支付宝转账时 {@code payeeName}（收款方真实姓名）为必填，防范盗号风险</li>
 *   <li>{@code outBizNo} 全局唯一，用于幂等控制，不可重复提交</li>
 * </ul>
 *
 * @author eagle
 */
@Data
@Builder
public class TransferRequest {

    /**
     * 商户转账流水号（全局唯一，用于幂等控制）
     */
    private String outBizNo;

    /**
     * 收款方账号（支付宝登录号 / 微信 openId）
     */
    private String payeeAccount;

    /**
     * 收款方真实姓名。
     *
     * <p>支付宝转账必填，用于校验身份防范盗号；
     * 微信支付根据配置可选。
     */
    private String payeeName;

    /**
     * 转账金额（元，精确到分）
     */
    private BigDecimal amount;

    /**
     * 转账备注（展示给收款方）
     */
    private String remark;
}
