package com.eagle.zhetaoke.jd.client;

import com.eagle.zhetaoke.dto.*;
import com.eagle.zhetaoke.jd.dto.JdOrderDetail;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 折京客 HTTP Service Interface 客户端。
 *
 * <p>基于 Spring Boot 4.0 {@link org.springframework.web.client.RestClient}
 * 声明式代理，覆盖折京客（京东）全站领券商品、转链、订单查询等全部 API。
 *
 * <p><b>使用方式：</b> 直接注入本接口即可调用：
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final ZhejingkeClient client;
 *
 *     public MyService(ZhejingkeClient client) {
 *         this.client = client;
 *     }
 *
 *     public void demo() {
 *         var resp = client.getAllItems("appkey", 1, 20);
 *         if (resp.isSuccess()) {
 *             resp.getContent().forEach(System.out::println);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author 孙士雄
 */
@HttpExchange(accept = "application/json")
public interface ZhejingkeClient {

    // ==================== 京东领券商品 API ====================

    /**
     * 全站领券商品 API（增量采集）。
     *
     * <p>接口地址：{@code /api/api_all.ashx}
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数（1-50）
     * @param sort     排序方式
     * @param cid      一级商品分类
     * @return 商品列表响应
     */
    @GetExchange("/api/api_all.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "cid", required = false) Integer cid);

    /**
     * 全网搜索商品 API。
     *
     * <p>接口地址：{@code /api/api_quanwang.ashx}
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @param sort     排序方式
     * @param q        搜索关键词
     * @return 商品列表响应
     */
    @GetExchange("/api/api_quanwang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "q", required = false) String q);

    /**
     * 全网商品详情 API。
     *
     * <p>接口地址：{@code /api/api_detail.ashx}
     *
     * @param appkey  折京客对接秘钥
     * @param skuId   商品 SKU ID
     * @return 商品详情响应
     */
    @GetExchange("/api/api_detail.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getItemDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("sku_id") String skuId);

    /**
     * 两小时销量榜 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_xiaoshi.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getHourlyRank(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 全天销量榜 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_quantian.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDailyRank(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 实时人气榜 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_shishi.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getRealTimeRank(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 实时支出佣金榜 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_yongjin.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getCommissionRank(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 9.9 元商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_jiu.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getNineItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 19.9 元商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_shijiu.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getNineteenItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 线报商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_xianbao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getXianbaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 咚咚抢商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_dongdong.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDongdongItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 今日商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_today.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getTodayItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 预告商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_yugao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getYugaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高佣金商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaoyongjin.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaoyongjinItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高销量商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaoxiaoliang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaoxiaoliangItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高评分商品 API。
     *
     * @param appkey   折京客对接秘钥
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaopingfen.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaopingfenItems(
            @RequestParam("appkey") String appkey,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 热搜词词典 API。
     *
     * @param appkey 折京客对接秘钥
     * @return 热搜词列表响应
     */
    @GetExchange("/api/api_guanjianci.ashx")
    ZhetaokeResponse<List<ZhetaokeHotWord>> getHotWords(
            @RequestParam("appkey") String appkey);

    /**
     * 联想词 API。
     *
     * @param appkey 折京客对接秘钥
     * @param q      搜索关键词
     * @return 联想词列表响应
     */
    @GetExchange("/api/api_suggest.ashx")
    ZhetaokeResponse<List<ZhetaokeSuggestWord>> getSuggestWords(
            @RequestParam("appkey") String appkey,
            @RequestParam("q") String q);

    // ==================== 京东转链 API ====================

    /**
     * 京东转链 API。
     *
     * @param appkey     折京客对接秘钥
     * @param materialId 推广物料 URL
     * @param unionId    京东联盟 ID
     * @param positionId 自定义推广位 ID（可选）
     * @param chainType  转链类型：1=长链，2=短链，3=长链+短链
     * @return 转链结果
     */
    @GetExchange("/api/open_jing_union_open_promotion_byunionid_get.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertJdLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("materialId") String materialId,
            @RequestParam("unionId") String unionId,
            @RequestParam(value = "positionId", required = false) String positionId,
            @RequestParam(value = "chainType", required = false) String chainType);

    // ==================== 京东订单查询 API ====================

    /**
     * 京东订单查询 API。
     *
     * @param appkey    折京客对接秘钥
     * @param unionId   京东联盟 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_jing_union_openz_order_row_query.ashx")
    ZhetaokeResponse<List<JdOrderDetail>> queryJdOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("unionId") String unionId,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 京东订单查询 API（新）。
     *
     * @param appkey    折京客对接秘钥
     * @param unionId   京东联盟 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_jd_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryJdOrders2(
            @RequestParam("appkey") String appkey,
            @RequestParam("unionId") String unionId,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 京东推广位创建 API。
     *
     * @param appkey     折京客对接秘钥
     * @param unionId    京东联盟 ID
     * @param key        推广位名称
     * @param unionType  推广位类型
     * @return 推广位信息
     */
    @GetExchange("/api/open_jing_union_open_position_create.ashx")
    ZhetaokeResponse<String> createJdPosition(
            @RequestParam("appkey") String appkey,
            @RequestParam("unionId") String unionId,
            @RequestParam("key") String key,
            @RequestParam(value = "unionType", required = false) String unionType);

    /**
     * 京东推广位查询 API。
     *
     * @param appkey  折京客对接秘钥
     * @param unionId 京东联盟 ID
     * @param page    分页页码
     * @param pageSize 每页条数
     * @return 推广位列表
     */
    @GetExchange("/api/open_jing_union_open_position_query.ashx")
    ZhetaokeResponse<List<String>> queryJdPositions(
            @RequestParam("appkey") String appkey,
            @RequestParam("unionId") String unionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);
}
