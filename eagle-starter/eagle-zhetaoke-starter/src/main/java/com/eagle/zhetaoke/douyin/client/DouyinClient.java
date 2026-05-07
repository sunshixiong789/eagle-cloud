package com.eagle.zhetaoke.douyin.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 抖音开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface DouyinClient {

    /**
     * 抖音商品转链 API。
     *
     * @param appkey        折淘客对接秘钥
     * @param sid           对应的淘客账号授权 SID
     * @param productUrl    商品 URL/口令/短链接
     * @param externalInfo  自定义字段
     * @param needQrCode    是否需要二维码
     * @param useCoupon     是否返回商品惠后价领券链接
     * @param needShareLink 是否返回站外 H5 链接
     * @return 转链结果
     */
    @GetExchange("/api/open_douyin_zhuanlian.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink2(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("product_url") String productUrl,
            @RequestParam(value = "external_info", required = false) String externalInfo,
            @RequestParam(value = "need_qr_code", required = false) String needQrCode,
            @RequestParam(value = "use_coupon", required = false) String useCoupon,
            @RequestParam(value = "need_share_link", required = false) String needShareLink);

    /**
     * 抖音商品详情 API。
     *
     * @param appkey      折淘客对接秘钥
     * @param sid         对应的淘客账号授权 SID
     * @param productIds  商品 ID 列表
     * @param fields      需要返回的字段
     * @return 商品详情列表
     */
    @GetExchange("/api/open_douyin_product_detail.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDouyinProductDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("product_ids") String productIds,
            @RequestParam(value = "fields", required = false) String fields);

    /**
     * 抖音商品搜索 API。
     *
     * @param appkey       折淘客对接秘钥
     * @param sid          对应的淘客账号授权 SID
     * @param title        商品标题
     * @param firstCids    筛选商品一级类目
     * @param secondCids   筛选商品二级类目
     * @param thirdCids    筛选商品三级类目
     * @param priceMin     筛选价格区间-最小值
     * @param priceMax     筛选价格区间-最大值
     * @param sellNumMin   筛选历史销量区间-最小值
     * @param sellNumMax   筛选历史销量区间-最大值
     * @param searchType   召回结果排序条件
     * @param sortType     排序顺序
     * @param cosFeeMin    筛选普通佣金区间-最小值
     * @param cosFeeMax    筛选普通佣金区间-最大值
     * @param cosRatioMin  筛选普通佣金率区间-最小值
     * @param cosRatioMax  筛选普通佣金率区间-最大值
     * @param page         分页
     * @param pageSize     每页的数量
     * @param shareStatus  获取商品分销状态
     * @param tag          商品标签
     * @return 商品列表
     */
    @GetExchange("/api/open_douyin_product_search.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchDouyinProducts(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "first_cids", required = false) String firstCids,
            @RequestParam(value = "second_cids", required = false) String secondCids,
            @RequestParam(value = "third_cids", required = false) String thirdCids,
            @RequestParam(value = "price_min", required = false) String priceMin,
            @RequestParam(value = "price_max", required = false) String priceMax,
            @RequestParam(value = "sell_num_min", required = false) String sellNumMin,
            @RequestParam(value = "sell_num_max", required = false) String sellNumMax,
            @RequestParam(value = "search_type", required = false) String searchType,
            @RequestParam(value = "sort_type", required = false) String sortType,
            @RequestParam(value = "cos_fee_min", required = false) String cosFeeMin,
            @RequestParam(value = "cos_fee_max", required = false) String cosFeeMax,
            @RequestParam(value = "cos_ratio_min", required = false) String cosRatioMin,
            @RequestParam(value = "cos_ratio_max", required = false) String cosRatioMax,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "page_size", required = false) String pageSize,
            @RequestParam(value = "share_status", required = false) String shareStatus,
            @RequestParam(value = "tag", required = false) String tag);

    /**
     * 抖音口令解析 API。
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     对应的淘客账号授权 SID
     * @param command 口令/短链接
     * @return 解析结果
     */
    @GetExchange("/api/open_douyin_kouling_jiexi.ashx")
    ZhetaokeResponse<ZhetaokeItem> parseDouyinCommand(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("command") String command);

    /**
     * 抖音订单查询 API（联盟订单）。
     *
     * @param appkey        折淘客对接秘钥
     * @param type          时间类型
     * @param page          页码
     * @param pageSize      每页数量
     * @param startTime     订单开始时间
     * @param endTime       订单结束时间
     * @param orderId       订单编号
     * @param sid           折淘客授权 sid
     * @param sanPingtaiId  订单所属平台 id
     * @return 订单列表
     */
    @GetExchange("/api/open_lianmeng_orderList.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryDouyinOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("type") String type,
            @RequestParam("page") String page,
            @RequestParam("page_size") String pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "orderid", required = false) String orderId,
            @RequestParam(value = "sid", required = false) String sid,
            @RequestParam(value = "san_pingtai_id", required = false) String sanPingtaiId);

    /**
     * 抖音直播间转链 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        对应的淘客账号授权 SID
     * @param buyinId    主播百应 ID
     * @param dyCode     直播间口令或者短链接
     * @param externalInfo 自定义字段
     * @return 转链结果
     */
    @GetExchange("/api/open_douyin_zhuanlian_zhibojian.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLiveLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "buyin_id", required = false) String buyinId,
            @RequestParam(value = "dy_code", required = false) String dyCode,
            @RequestParam(value = "external_info", required = false) String externalInfo);
}
