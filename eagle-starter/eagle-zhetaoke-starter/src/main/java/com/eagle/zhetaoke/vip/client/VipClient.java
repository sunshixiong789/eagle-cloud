package com.eagle.zhetaoke.vip.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 唯品会开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface VipClient {

    /**
     * 唯品会转链 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param sid       对应的唯品会账号授权 SID
     * @param url       唯品会商品 id 或者唯品会链接
     * @param chanTag   渠道标识
     * @param statParam 自定义渠道统计参数
     * @return 转链结果
     */
    @GetExchange("/api/open_vip_genByVIPUrlWithOauth.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("url") String url,
            @RequestParam(value = "chanTag", required = false) String chanTag,
            @RequestParam(value = "statParam", required = false) String statParam);

    /**
     * 唯品会账号授权 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 授权结果
     */
    @GetExchange("/api/vip_shouquan.ashx")
    ZhetaokeResponse<String> authorizeVipAccount(
            @RequestParam("appkey") String appkey);

    /**
     * 唯品会获取授权列表 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param page      第几页
     * @param expireDay 还剩下几天授权过期
     * @param sid       对应的唯品会账号授权 ID
     * @return 授权列表
     */
    @GetExchange("/api/vip_shouquaninfo.ashx")
    ZhetaokeResponse<List<String>> getVipAuthorizationList(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "expire_day", required = false) String expireDay,
            @RequestParam(value = "sid", required = false) String sid);

    /**
     * 唯品会获取订单列表 API。
     *
     * @param appkey         折淘客对接秘钥
     * @param sid            对应的唯品会账号授权 SID
     * @param status         订单状态
     * @param orderTimeStart 订单时间起始
     * @param orderTimeEnd   订单时间结束
     * @param page           页码
     * @param pageSize       页面大小
     * @param updateTimeStart 更新时间-起始
     * @param updateTimeEnd   更新时间-结束
     * @param orderSnList    订单号列表
     * @param vendorCode     vendorCode
     * @param chanTag        渠道标识
     * @return 订单列表
     */
    @GetExchange("/api/open_vip_orderListWithOauth.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryVipOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderTimeStart", required = false) String orderTimeStart,
            @RequestParam(value = "orderTimeEnd", required = false) String orderTimeEnd,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "updateTimeStart", required = false) String updateTimeStart,
            @RequestParam(value = "updateTimeEnd", required = false) String updateTimeEnd,
            @RequestParam(value = "orderSnList", required = false) String orderSnList,
            @RequestParam(value = "vendorCode", required = false) String vendorCode,
            @RequestParam(value = "chanTag", required = false) String chanTag);

    /**
     * 唯品会商品详情 API（老版本）。
     *
     * @param appkey 折淘客对接秘钥
     * @param sid    对应的唯品会账号授权 SID
     * @param id     唯品会商品 id 或者唯品会链接
     * @return 商品详情
     */
    @GetExchange("/api/open_vip_getByGoodsIdsWithOauth.ashx")
    ZhetaokeResponse<ZhetaokeItem> getVipGoodsDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("id") String id);

    /**
     * 唯品会精编商品文案 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param page       第几页
     * @param pageSize   每页数据条数
     * @param sort       商品排序方式
     * @param totalCount 是否获取商品总数
     * @return 商品文案列表
     */
    @GetExchange("/api/open_vip_wenan_list.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getVipWenanList(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "page_size", required = false) String pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "total_count", required = false) String totalCount);

    /**
     * 唯品会关键词查询商品 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        对应的唯品会账号授权 SID
     * @param keyword    搜索关键词
     * @param fieldName  价格排序：PRICE，DISCOUNT：折扣排序，销量排序：SALES
     * @param order      排序顺序
     * @param page       第几页
     * @param pageSize   每页数据条数
     * @param priceStart 价格区间 start
     * @param priceEnd   价格区间 end
     * @return 商品列表
     */
    @GetExchange("/api/open_vip_queryWithOauth.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchVipGoods(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "fieldName", required = false) String fieldName,
            @RequestParam(value = "order", required = false) String order,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "pageSize", required = false) String pageSize,
            @RequestParam(value = "priceStart", required = false) String priceStart,
            @RequestParam(value = "priceEnd", required = false) String priceEnd);

    /**
     * 唯品会商品详情 V2 新 API。
     *
     * @param appkey                     折淘客对接秘钥
     * @param sid                        对应的唯品会账号授权 SID
     * @param id                         唯品会商品 id 或者唯品会链接
     * @param queryDetail                是否查询详情信息
     * @param queryStock                 是否查询商品库存状态
     * @param queryReputation            是否查询商品评价信息
     * @param queryStoreServiceCapability 是否查询商品所属店铺服务能力信息
     * @param queryPMSAct                是否查询商品活动信息
     * @param extendBySpu                是否按照同 spu 扩展
     * @param queryExclusiveCoupon       是否查询渠道专属红包信息
     * @param extendSku                  是否扩展查询商品 sku 信息
     * @return 商品详情
     */
    @GetExchange("/api/open_vip_getByGoodsIdsV2WithOauth.ashx")
    ZhetaokeResponse<ZhetaokeItem> getVipGoodsDetailV2(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("id") String id,
            @RequestParam(value = "queryDetail", required = false) String queryDetail,
            @RequestParam(value = "queryStock", required = false) String queryStock,
            @RequestParam(value = "queryReputation", required = false) String queryReputation,
            @RequestParam(value = "queryStoreServiceCapability", required = false) String queryStoreServiceCapability,
            @RequestParam(value = "queryPMSAct", required = false) String queryPMSAct,
            @RequestParam(value = "extendBySpu", required = false) String extendBySpu,
            @RequestParam(value = "queryExclusiveCoupon", required = false) String queryExclusiveCoupon,
            @RequestParam(value = "extendSku", required = false) String extendSku);
}
