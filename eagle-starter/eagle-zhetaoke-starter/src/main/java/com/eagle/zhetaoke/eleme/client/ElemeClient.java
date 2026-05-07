package com.eagle.zhetaoke.eleme.client;

import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 饿了么（淘宝闪购）开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface ElemeClient {

    /**
     * 饿了么（淘宝闪购）转链 API。
     *
     * @param appkey      折淘客对接秘钥
     * @param sid         对应的淘客账号授权 SID
     * @param activityId  饿了么相关活动 id
     * @param customerId  自定义参数
     * @return 转链结果
     */
    @GetExchange("/api/open_eleme_generateLink.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> generateElemeLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("activity_id") String activityId,
            @RequestParam(value = "customer_id", required = false) String customerId);

    /**
     * 饿了么订单查询 API（联盟订单）。
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
    ZhetaokeResponse<List<ZhetaokeOrder>> queryElemeOrders2(
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
