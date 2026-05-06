package com.eagle.zhetaoke.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品筛选查询请求参数。
 *
 * <p>继承 {@link BaseQueryRequest}，增加全站领券商品 API 的筛选条件。
 *
 * @author 孙士雄
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ItemFilterRequest extends BaseQueryRequest {

    private static final long serialVersionUID = 1L;

    /** 一级商品分类。 */
    private Integer cid;

    /** 是否天猫：tmall / gold_seller。 */
    private String tj;

    /** 淘抢购/聚划算：taoqianggou / juhuasuan。 */
    private String jt;

    /** 海淘/极有家：haitao / jiyoujia。 */
    private String jh;

    /** 今日商品：1。 */
    private String today;

    /** 运费险：1。 */
    private String yunfeixian;

    /** 精选品牌：1。 */
    private String pinpai;

    /** 天猫超市：1。 */
    private String tianmaochaoshi;

    /** 价格区间，如 "0.0-9.9"。 */
    private String price;

    /** 佣金比例≥。 */
    private String commissionRateStart;

    /** 年销量≥。 */
    private String saleNumStart;

    /** 动态评分≥。 */
    private String dsrStart;

    /** 券面额≥。 */
    private String couponAmountStart;

    /** 关键词。 */
    private String q;

    /** 极品爆单：1。 */
    private String baodan;

    /** 偏远地区包邮：1。 */
    private String pyBaoyou;

    /** 朋友圈火爆：1。 */
    private String tag;

    /** 潮电街：1。 */
    private String ifashion;

    /** 数据更新开始时间。 */
    private String startDateTimeYongjin;

    /** 数据更新结束时间。 */
    private String endDateTimeYongjin;

    /** 任务专属商品：2。 */
    private String mission;

    /** 是否返回总数：1。 */
    private Integer totalCount;
}
