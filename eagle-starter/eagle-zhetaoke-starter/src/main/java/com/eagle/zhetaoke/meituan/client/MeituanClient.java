package com.eagle.zhetaoke.meituan.client;

import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 美团开放平台 API 客户端。
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface MeituanClient {

    /**
     * 美团转链 API。
     *
     * @param appkey      折淘客对接秘钥
     * @param sid         对应的淘客账号授权 SID
     * @param actId       美团活动 id
     * @param linkType    链接类型
     * @param miniCode    是否生成小程序二维码
     * @param miniCode2   是否生成长链接二维码推广大图
     * @param miniCode3   是否生成小程序二维码推广大图
     * @param platform    自定义平台名称
     * @param customerId  自定义参数
     * @return 转链结果
     */
    @GetExchange("/api/open_meituan_generateLink.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> generateMeituanLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("actId") String actId,
            @RequestParam("linkType") String linkType,
            @RequestParam(value = "miniCode", required = false) String miniCode,
            @RequestParam(value = "miniCode2", required = false) String miniCode2,
            @RequestParam(value = "miniCode3", required = false) String miniCode3,
            @RequestParam(value = "platform", required = false) String platform,
            @RequestParam(value = "customer_id", required = false) String customerId);

    /**
     * 美团订单查询 API（新）。
     *
     * @param appkey    折淘客对接秘钥
     * @param type      时间类型
     * @param page      页码
     * @param pageSize  每页数量
     * @param startTime 订单开始时间
     * @param endTime   订单结束时间
     * @param orderId   订单编号
     * @param sid       折淘客授权 sid
     * @return 订单列表
     */
    @GetExchange("/api/open_meituan_orderList2.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("type") String type,
            @RequestParam("page") String page,
            @RequestParam("page_size") String pageSize,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "orderid", required = false) String orderId,
            @RequestParam(value = "sid", required = false) String sid);
}
