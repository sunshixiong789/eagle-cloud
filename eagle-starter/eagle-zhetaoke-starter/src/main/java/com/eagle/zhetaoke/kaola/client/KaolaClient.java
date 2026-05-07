package com.eagle.zhetaoke.kaola.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 考拉开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface KaolaClient {

    /**
     * 考拉转链 API。
     *
     * @param appkey        折淘客对接秘钥
     * @param sid           对应的淘客账号授权 SID
     * @param targetUrl     考拉商品 id 或者考拉链接
     * @param trackingCode2 自定义参数
     * @return 转链结果
     */
    @GetExchange("/api/open_kaola_zhuanke_api_zhuanlian.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink2(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("targetUrl") String targetUrl,
            @RequestParam(value = "trackingCode2", required = false) String trackingCode2);

    /**
     * 考拉精选商品列表 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param poolName 精选商品池名称
     * @param pageNo   第几页
     * @param pageSize 每页数据条数
     * @return 商品列表
     */
    @GetExchange("/api/open_kaola_zhuanke_api_querySelectedGoods.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getKaolaSelectedGoods(
            @RequestParam("appkey") String appkey,
            @RequestParam("poolName") String poolName,
            @RequestParam(value = "pageNo", required = false) String pageNo,
            @RequestParam(value = "pageSize", required = false) String pageSize);

    /**
     * 考拉关键词查询商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param keyWord  搜索关键词
     * @param type     排序方式
     * @param desc     是否降序
     * @param pageNo   第几页
     * @param pageSize 每页数据条数
     * @return 商品列表
     */
    @GetExchange("/api/open_kaola_zhuanke_api_searchGoods.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchKaolaGoods(
            @RequestParam("appkey") String appkey,
            @RequestParam("keyWord") String keyWord,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "desc", required = false) String desc,
            @RequestParam(value = "pageNo", required = false) String pageNo,
            @RequestParam(value = "pageSize", required = false) String pageSize);

    /**
     * 考拉商品详情 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param goodsIds 考拉商品 id
     * @return 商品详情
     */
    @GetExchange("/api/open_kaola_zhuanke_api_queryGoodsInfo.ashx")
    ZhetaokeResponse<ZhetaokeItem> getKaolaGoodsDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("goodsIds") String goodsIds);

    /**
     * 考拉订单查询 API（联盟订单）。
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
    ZhetaokeResponse<List<ZhetaokeOrder>> queryKaolaOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("type") String type,
            @RequestParam("page") String page,
            @RequestParam("page_size") String pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "orderid", required = false) String orderId,
            @RequestParam(value = "sid", required = false) String sid,
            @RequestParam(value = "san_pingtai_id", required = false) String sanPingtaiId);
}
