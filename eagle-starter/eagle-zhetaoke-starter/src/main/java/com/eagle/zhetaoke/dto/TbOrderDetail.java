package com.eagle.zhetaoke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 淘宝联盟订单详情。
 *
 * <p>淘宝订单查询 API 返回的订单数据结构。
 *
 * @author 孙士雄
 */
@Data
public class TbOrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 淘宝付款时间。 */
    @JsonProperty("tb_paid_time")
    private String tbPaidTime;

    /** 淘客付款时间。 */
    @JsonProperty("tk_paid_time")
    private String tkPaidTime;

    /** 付款金额。 */
    @JsonProperty("pay_price")
    private String payPrice;

    /** 付款预估收入。 */
    @JsonProperty("pub_share_fee")
    private String pubShareFee;

    /** 订单编号。 */
    @JsonProperty("trade_id")
    private String tradeId;

    /** 淘客订单状态，12-付款，13-关闭，14-确认收货，3-结算成功。 */
    @JsonProperty("tk_status")
    private Integer tkStatus;

    /** 淘客订单角色，2-二方，3-三方。 */
    @JsonProperty("tk_order_role")
    private Integer tkOrderRole;

    /** 淘客赚取时间。 */
    @JsonProperty("tk_earning_time")
    private String tkEarningTime;

    /** 广告位 ID。 */
    @JsonProperty("adzone_id")
    private Long adzoneId;

    /** 广告位名称。 */
    @JsonProperty("adzone_name")
    private String adzoneName;

    /** 分成比率。 */
    @JsonProperty("pub_share_rate")
    private String pubShareRate;

    /** 退款标识，0-正常，1-退款。 */
    @JsonProperty("refund_tag")
    private Integer refundTag;

    /** 补贴比率。 */
    @JsonProperty("subsidy_rate")
    private String subsidyRate;

    /** 总佣金比率。 */
    @JsonProperty("tk_total_rate")
    private String tkTotalRate;

    /** 商品类目名称。 */
    @JsonProperty("item_category_name")
    private String itemCategoryName;

    /** 卖家昵称。 */
    @JsonProperty("seller_nick")
    private String sellerNick;

    /** 推广者 ID。 */
    @JsonProperty("pub_id")
    private Long pubId;

    /** 商品图片。 */
    @JsonProperty("item_img")
    private String itemImg;

    /** 预估收入。 */
    @JsonProperty("pub_share_pre_fee")
    private String pubSharePreFee;

    /** 支付宝总支付金额。 */
    @JsonProperty("alipay_total_price")
    private String alipayTotalPrice;

    /** 商品标题。 */
    @JsonProperty("item_title")
    private String itemTitle;

    /** 媒体名称。 */
    @JsonProperty("site_name")
    private String siteName;

    /** 商品数量。 */
    @JsonProperty("item_num")
    private Integer itemNum;

    /** 补贴金额。 */
    @JsonProperty("subsidy_fee")
    private String subsidyFee;

    /** 淘客赚取的佣金。 */
    @JsonProperty("alimama_share_fee")
    private String alimamaShareFee;

    /** 父订单编号。 */
    @JsonProperty("trade_parent_id")
    private String tradeParentId;

    /** 订单类型。 */
    @JsonProperty("order_type")
    private String orderType;

    /** 淘客创建时间。 */
    @JsonProperty("tk_create_time")
    private String tkCreateTime;

    /** 流量来源。 */
    @JsonProperty("flow_source")
    private String flowSource;

    /** 终端类型。 */
    @JsonProperty("terminal_type")
    private String terminalType;

    /** 点击时间。 */
    @JsonProperty("click_time")
    private String clickTime;

    /** 商品单价。 */
    @JsonProperty("item_price")
    private String itemPrice;

    /** 商品 ID。 */
    @JsonProperty("item_id")
    private Long itemId;

    /** 推广位 ID。 */
    @JsonProperty("site_id")
    private Long siteId;

    /** 店铺名称。 */
    @JsonProperty("seller_shop_title")
    private String sellerShopTitle;

    /** 收入比率。 */
    @JsonProperty("income_rate")
    private String incomeRate;

    /** 总佣金金额。 */
    @JsonProperty("total_commission_fee")
    private String totalCommissionFee;

    /** 佣金比率。 */
    @JsonProperty("total_commission_rate")
    private String totalCommissionRate;

    /** 媒体平台预估佣金。 */
    @JsonProperty("tk_commission_pre_fee_for_media_platform")
    private String tkCommissionPreFeeForMediaPlatform;

    /** 媒体平台佣金。 */
    @JsonProperty("tk_commission_fee_for_media_platform")
    private String tkCommissionFeeForMediaPlatform;

    /** 媒体平台佣金比率。 */
    @JsonProperty("tk_commission_rate_for_media_platform")
    private String tkCommissionRateForMediaPlatform;

    /** 会员运营 ID。 */
    @JsonProperty("special_id")
    private Long specialId;

    /** 渠道关系 ID。 */
    @JsonProperty("relation_id")
    private Long relationId;
}
