package com.eagle.zhetaoke.jd.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 京东开放平台补充 API 客户端。
 *
 * <p>覆盖 api3.aspx 页面中的京东商品详情、精选商品、朋友圈商品、礼金商品等 API。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface JdOpenClient {

    /**
     * 京东转链 API（旧版）。
     *
     * @param appkey     折淘客对接秘钥
     * @param content    京东 url 链接
     * @param jdLianmengId 京东联盟 ID
     * @param positionId 自定义推广位 ID
     * @param couponUrl  优惠券领取链接
     * @return 转链结果
     */
    @GetExchange("/api/open_jing_zhuanlian.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertJdLinkOld(
            @RequestParam("appkey") String appkey,
            @RequestParam("content") String content,
            @RequestParam("jd_lianmeng_id") String jdLianmengId,
            @RequestParam(value = "positionId", required = false) String positionId,
            @RequestParam(value = "couponUrl", required = false) String couponUrl);

    /**
     * 京东商品详情 API[简版]。
     *
     * @param appkey  折淘客对接秘钥
     * @param content 京东商品 id 或者京东 url 链接
     * @return 商品详情
     */
    @GetExchange("/api/open_jing_goods_detail_get.ashx")
    ZhetaokeResponse<ZhetaokeItem> getJdGoodsDetailSimple(
            @RequestParam("appkey") String appkey,
            @RequestParam("content") String content);

    /**
     * 京东商品详情 API[详版]。
     *
     * @param appkey               折淘客对接秘钥
     * @param jdAppKey             京东开放平台应用的 appkey
     * @param jdAppSecret          京东开放平台应用的 appsecret
     * @param cid1                 一级类目 id
     * @param cid2                 二级类目 id
     * @param cid3                 三级类目 id
     * @param pageIndex            页码
     * @param pageSize             每页数量
     * @param skuIds               skuid 集合
     * @param keyword              关键词
     * @param pricefrom            商品价格下限
     * @param priceto              商品价格上限
     * @param commissionShareStart 佣金比例区间开始
     * @param commissionShareEnd   佣金比例区间结束
     * @param owner                商品类型：自营[g]，POP[p]
     * @param sortName             排序字段
     * @param sort                 asc,desc 升降序
     * @param isCoupon             是否是优惠券商品
     * @param isPG                 是否是拼购商品
     * @param shopId               店铺 Id
     * @param fields               支持出参数据筛选
     * @param deliveryType         京东配送 1：是，0：不是
     * @param area                 区域
     * @return 商品详情列表
     */
    @GetExchange("/api/open_jing_union_open_goods_query.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJdGoodsDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "jd_app_key", required = false) String jdAppKey,
            @RequestParam(value = "jd_app_secret", required = false) String jdAppSecret,
            @RequestParam(value = "cid1", required = false) String cid1,
            @RequestParam(value = "cid2", required = false) String cid2,
            @RequestParam(value = "cid3", required = false) String cid3,
            @RequestParam(value = "pageIndex", required = false) String pageIndex,
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "skuIds", required = false) String skuIds,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pricefrom", required = false) String pricefrom,
            @RequestParam(value = "priceto", required = false) String priceto,
            @RequestParam(value = "commissionShareStart", required = false) String commissionShareStart,
            @RequestParam(value = "commissionShareEnd", required = false) String commissionShareEnd,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "sortName", required = false) String sortName,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "isCoupon", required = false) String isCoupon,
            @RequestParam(value = "isPG", required = false) String isPG,
            @RequestParam(value = "shopId", required = false) String shopId,
            @RequestParam(value = "fields", required = false) String fields,
            @RequestParam(value = "deliveryType", required = false) String deliveryType,
            @RequestParam(value = "area", required = false) String area);

    /**
     * 京粉精选商品 API。
     *
     * @param appkey      折淘客对接秘钥
     * @param eliteId     频道 ID
     * @param pageIndex   页码
     * @param pageSize    每页数量
     * @param sortName    排序字段
     * @param sort        升降序
     * @param pid         联盟 id_应用 id_推广位 id
     * @param fields      支持出参数据筛选
     * @param forbidTypes 禁售类型
     * @return 商品列表
     */
    @GetExchange("/api/open_jing_union_open_goods_jingfen_query.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJingfenGoods(
            @RequestParam("appkey") String appkey,
            @RequestParam("eliteId") String eliteId,
            @RequestParam(value = "pageIndex", required = false) String pageIndex,
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "sortName", required = false) String sortName,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "pid", required = false) String pid,
            @RequestParam(value = "fields", required = false) String fields,
            @RequestParam(value = "forbidTypes", required = false) String forbidTypes);

    /**
     * 京东朋友圈火爆商品 API。
     *
     * @param pageSize             每页数量
     * @param goodsId              商品 ID
     * @param keyword              关键词
     * @param goodsType            一级类目
     * @param commissionRateStart  佣金比例≥
     * @param saleNumStart         年销量≥
     * @param price                价格区间
     * @param couponAmountStart    券面额≥
     * @param sort                 商品排序方式
     * @param totalCount           返回商品总数
     * @return 商品列表
     */
    @GetExchange("/api/open_jing_goods_list.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJdPyqItems(
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "goods_id", required = false) String goodsId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "goods_type", required = false) String goodsType,
            @RequestParam(value = "commission_rate_start", required = false) String commissionRateStart,
            @RequestParam(value = "sale_num_start", required = false) String saleNumStart,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "coupon_amount_start", required = false) String couponAmountStart,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "total_count", required = false) String totalCount);

    /**
     * 京享礼金商品 API。
     *
     * @param pageSize            每页数量
     * @param keyword             关键词
     * @param commissionRateStart 佣金比例≥
     * @param saleNumStart        年销量≥
     * @param price               价格区间
     * @param couponAmountStart   券面额≥
     * @param sort                商品排序方式
     * @param totalCount          返回商品总数
     * @return 商品列表
     */
    @GetExchange("/api/open_jing_goods_lijin_list.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJdLiJinItems(
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "commission_rate_start", required = false) String commissionRateStart,
            @RequestParam(value = "sale_num_start", required = false) String saleNumStart,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "coupon_amount_start", required = false) String couponAmountStart,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "total_count", required = false) String totalCount);

    /**
     * 京东商品详情 API[详情图]。
     *
     * @param appkey  折淘客对接秘钥
     * @param content 京东商品字符串 id 或者京东 url 链接
     * @param fields  查询域集合
     * @return 商品大字段详情
     */
    @GetExchange("/api/open_jd_union_open_goods_bigfield_query.ashx")
    ZhetaokeResponse<ZhetaokeItem> getJdGoodsBigField(
            @RequestParam("appkey") String appkey,
            @RequestParam("content") String content,
            @RequestParam(value = "fields", required = false) String fields);
}
