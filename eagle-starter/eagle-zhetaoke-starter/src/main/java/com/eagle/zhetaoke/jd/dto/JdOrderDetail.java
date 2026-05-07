package com.eagle.zhetaoke.jd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 京东联盟订单详情。
 *
 * <p>京东订单查询 API 返回的订单数据结构。
 *
 * @author 孙士雄
 */
@Data
public class JdOrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号。 */
    private String orderId;

    /** 父订单号。 */
    private String parentId;

    /** 商品 SKU ID。 */
    private String skuId;

    /** 商品名称。 */
    private String skuName;

    /** 商品数量。 */
    private Integer skuNum;

    /** 商品单价。 */
    private String skuPrice;

    /** 商品图片。 */
    private String skuImg;

    /** 订单金额。 */
    private String orderPrice;

    /** 佣金金额。 */
    private String commission;

    /** 佣金比例。 */
    private String commissionRate;

    /** 订单状态：2-待付款，3-已付款，4-已取消，5-已完成，6-已结算。 */
    private Integer orderStatus;

    /** 下单时间。 */
    private String orderTime;

    /** 完成时间。 */
    private String finishTime;

    /** 结算时间。 */
    private String settleTime;

    /** 联盟 ID。 */
    private String unionId;

    /** 推广位 ID。 */
    private String positionId;

    /** 子联盟 ID。 */
    private String subUnionId;

    /** 实际佣金。 */
    private String actualCommission;

    /** 实际佣金比例。 */
    private String actualCommissionRate;

    /** 补贴金额。 */
    private String subsidyAmount;

    /** 补贴比例。 */
    private String subsidyRate;

    /** 预估佣金。 */
    private String estimateCommission;

    /** 预估佣金比例。 */
    private String estimateCommissionRate;

    /** 预估趣享佣金。 */
    private String estimateCosPrice;

    /** 实际趣享佣金。 */
    private String actualCosPrice;

    /** 商品链接。 */
    private String skuUrl;

    /** 商品类目 ID。 */
    private String cid1;

    /** 商品类目名称。 */
    private String cid1Name;

    /** 商家 ID。 */
    private String merchantId;

    /** 商家名称。 */
    private String merchantName;

    /** 是否自营：1-自营，0-非自营。 */
    private Integer selfSupport;

    /** 平台类型：jd。 */
    private String platform = "jd";
}
