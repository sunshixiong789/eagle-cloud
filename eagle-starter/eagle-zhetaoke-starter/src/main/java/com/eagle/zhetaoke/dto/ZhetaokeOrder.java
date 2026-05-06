package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 折淘客订单信息。
 *
 * <p>覆盖京东、美团、饿了么、唯品会、考拉、拼多多、抖音等平台的订单查询响应。
 *
 * @author 孙士雄
 */
@Data
public class ZhetaokeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单号。 */
    private String orderId;

    /** 商品 ID。 */
    private String itemId;

    /** 商品标题。 */
    private String itemTitle;

    /** 商品图片。 */
    private String itemImg;

    /** 订单金额。 */
    private String orderAmount;

    /** 佣金金额。 */
    private String commissionAmount;

    /** 佣金比例。 */
    private String commissionRate;

    /** 订单状态。 */
    private String orderStatus;

    /** 下单时间。 */
    private String orderTime;

    /** 结算时间。 */
    private String settleTime;

    /** 平台类型：jd、mt、elm、vip、kaola、pdd、douyin。 */
    private String platform;

    /** 子推客身份标识。 */
    private String positionId;

    /** 自定义推广位 ID。 */
    private String pid;
}
