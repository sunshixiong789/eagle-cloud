package com.eagle.zhetaoke.pdd.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 拼多多开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface PddClient {

    /**
     * 拼多多高效转链 API。
     *
     * @param appkey          折淘客对接秘钥
     * @param pddAppKey       拼多多开放平台应用的 appkey
     * @param pddAppSecret    拼多多开放平台应用的 appsecret
     * @param pid             拼多多平台的推广位
     * @param content         支持纯数字 id 或者拼多多推广短链
     * @param customParameters 自定义参数
     * @return 转链结果
     */
    @GetExchange("/api/open_pdd_zhuanlian_new.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertPddLink2(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("pid") String pid,
            @RequestParam("content") String content,
            @RequestParam(value = "custom_parameters", required = false) String customParameters);

    /**
     * 拼多多商品详情 API（简版）。
     *
     * @param appkey        折淘客对接秘钥
     * @param pddAppKey     拼多多开放平台应用的 appkey
     * @param pddAppSecret  拼多多开放平台应用的 appsecret
     * @param pid           拼多多平台的推广位
     * @param content       支持纯数字 id
     * @return 商品详情
     */
    @GetExchange("/api/open_pdd_goods_detail_get_new.ashx")
    ZhetaokeResponse<ZhetaokeItem> getPddGoodsDetailSimple(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("pid") String pid,
            @RequestParam("content") String content);

    /**
     * 拼多多授权备案 API。
     *
     * @param appkey           折淘客对接秘钥
     * @param pddAppKey        拼多多开放平台应用的 appkey
     * @param pddAppSecret     拼多多开放平台应用的 appsecret
     * @param pid              拼多多平台的推广位
     * @param customParameters 自定义参数
     * @param generateQqApp    是否生成 qq 小程序
     * @param generateWeApp    是否生成拼多多福利券微信小程序推广信息
     * @return 授权结果
     */
    @GetExchange("/api/open_pdd_shouquan_new.ashx")
    ZhetaokeResponse<String> authorizePddAccount(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("pid") String pid,
            @RequestParam(value = "custom_parameters", required = false) String customParameters,
            @RequestParam(value = "generate_qq_app", required = false) String generateQqApp,
            @RequestParam(value = "generate_we_app", required = false) String generateWeApp);

    /**
     * 拼多多授权备案查询 API。
     *
     * @param appkey           折淘客对接秘钥
     * @param pddAppKey        拼多多开放平台应用的 appkey
     * @param pddAppSecret     拼多多开放平台应用的 appsecret
     * @param pid              拼多多平台的推广位
     * @param customParameters 自定义参数
     * @return 备案查询结果
     */
    @GetExchange("/api/open_pdd_shouquan_query_new.ashx")
    ZhetaokeResponse<String> queryPddAuthStatus(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("pid") String pid,
            @RequestParam(value = "custom_parameters", required = false) String customParameters);

    /**
     * 拼多多订单查询 API（新）。
     *
     * @param appkey           折淘客对接秘钥
     * @param pddAppKey        拼多多开放平台应用的 appkey
     * @param pddAppSecret     拼多多开放平台应用的 appsecret
     * @param startUpdateTime  最近90天内多多进宝商品订单更新时间--查询时间开始
     * @param endUpdateTime    最近90天内多多进宝商品订单更新时间--查询时间结束
     * @param queryOrderType   订单类型
     * @param page             第几页
     * @param pageSize         返回的每页结果订单数
     * @param cashGiftOrder    是否为礼金订单
     * @return 订单列表
     */
    @GetExchange("/api/open_pdd_dingdan_new.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryPddOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("start_update_time") String startUpdateTime,
            @RequestParam("end_update_time") String endUpdateTime,
            @RequestParam(value = "query_order_type", required = false) String queryOrderType,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "page_size", required = false) String pageSize,
            @RequestParam(value = "cash_gift_order", required = false) String cashGiftOrder);

    /**
     * 拼多多商品详情查询 API（详版）。
     *
     * @param appkey           折淘客对接秘钥
     * @param pddAppKey        拼多多开放平台应用的 appkey
     * @param pddAppSecret     拼多多开放平台应用的 appsecret
     * @param pid              拼多多平台的推广位
     * @param customParameters 自定义参数
     * @param keyword          商品关键词
     * @param catId            商品类目 ID
     * @param activityTags     活动商品标记数组
     * @param blockCatPackages 屏蔽商品类目包
     * @param blockCats        自定义屏蔽一级/二级/三级类目 ID
     * @param isBrandGoods     是否为品牌商品
     * @param merchantType     店铺类型
     * @param sortType         排序方式
     * @param useCustomized    是否使用个性化推荐
     * @param withCoupon       是否只返回优惠券的商品
     * @return 商品详情列表
     */
    @GetExchange("/api/open_pdd_goods_detail_search_new.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchPddGoodsDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("pid") String pid,
            @RequestParam(value = "custom_parameters", required = false) String customParameters,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "cat_id", required = false) String catId,
            @RequestParam(value = "activity_tags", required = false) String activityTags,
            @RequestParam(value = "block_cat_packages", required = false) String blockCatPackages,
            @RequestParam(value = "block_cats", required = false) String blockCats,
            @RequestParam(value = "is_brand_goods", required = false) String isBrandGoods,
            @RequestParam(value = "merchant_type", required = false) String merchantType,
            @RequestParam(value = "sort_type", required = false) String sortType,
            @RequestParam(value = "use_customized", required = false) String useCustomized,
            @RequestParam(value = "with_coupon", required = false) String withCoupon);

    /**
     * 拼多多商品标准类目接口。
     *
     * @param appkey        折淘客对接秘钥
     * @param pddAppKey     拼多多开放平台应用的 appkey
     * @param pddAppSecret  拼多多开放平台应用的 appsecret
     * @param parentCatId   值=0 时为顶点 cat_id
     * @return 类目列表
     */
    @GetExchange("/api/open_pdd_goods_cats_new.ashx")
    ZhetaokeResponse<List<String>> getPddGoodsCats(
            @RequestParam("appkey") String appkey,
            @RequestParam("pdd_app_key") String pddAppKey,
            @RequestParam("pdd_app_secret") String pddAppSecret,
            @RequestParam("parent_cat_id") String parentCatId);
}
