package com.eagle.zhetaoke.client;

import com.eagle.zhetaoke.dto.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 折淘客 HTTP Service Interface 客户端。
 *
 * <p>基于 Spring Boot 4.0 {@link org.springframework.web.client.RestClient}
 * 声明式代理，覆盖折淘客全站领券商品、转链、订单查询等全部 API。
 *
 * <p><b>使用方式：</b> 直接注入本接口即可调用：
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final ZhetaokeClient client;
 *
 *     public MyService(ZhetaokeClient client) {
 *         this.client = client;
 *     }
 *
 *     public void demo() {
 *         var resp = client.getAllItems("appkey", "sid", "pid", 1, 20, "new", null, null, null, null);
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
public interface ZhetaokeClient {

    // ==================== 淘宝领券商品 API ====================

    /**
     * 全站领券商品 API（增量采集）。
     *
     * <p>接口地址：{@code /api/api_all.ashx}
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数（1-50）
     * @param sort     排序方式
     * @param cid      一级商品分类
     * @param tj       是否天猫：tmall/gold_seller
     * @param jt       淘抢购/聚划算：taoqianggou/juhuasuan
     * @param jh       海淘/极有家：haitao/jiyoujia
     * @return 商品列表响应
     */
    @GetExchange("/api/api_all.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "cid", required = false) Integer cid,
            @RequestParam(value = "tj", required = false) String tj,
            @RequestParam(value = "jt", required = false) String jt,
            @RequestParam(value = "jh", required = false) String jh);

    /**
     * 全网搜索商品 API。
     *
     * <p>接口地址：{@code /api/api_quanwang.ashx}
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @param sort     排序方式
     * @param q        搜索关键词
     * @return 商品列表响应
     */
    @GetExchange("/api/api_quanwang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> searchItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "q", required = false) String q);

    /**
     * 全网商品详情 API。
     *
     * <p>接口地址：{@code /api/api_detail.ashx}
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     淘客账号授权 ID
     * @param pid     淘客 PID
     * @param taoId   商品 ID
     * @param code    折淘客编号（可选）
     * @param numIids 多个商品 ID 串（可选）
     * @return 商品详情响应
     */
    @GetExchange("/api/api_detail.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getItemDetail(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("tao_id") String taoId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "num_iids", required = false) String numIids);

    /**
     * 两小时销量榜 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_xiaoshi.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getHourlyRank(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 全天销量榜 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_quantian.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDailyRank(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 实时人气榜 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_shishi.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getRealTimeRank(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 实时支出佣金榜 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_yongjin.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getCommissionRank(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 9.9 元商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_jiu.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getNineItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 19.9 元商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_shijiu.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getNineteenItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 视频（抖货）商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_videos.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getVideoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 线报商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_xianbao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getXianbaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 咚咚抢商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_dongdong.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDongdongItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 朋友圈火爆商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_pengyouquan.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getPyqItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 天猫商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_tmall.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getTmallItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 金牌卖家商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gold.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGoldItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 淘抢购商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_taoqianggou.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getTaoqianggouItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 聚划算商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_juhuasuan.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJuhuasuanItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 海淘商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_haitao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getHaitaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 极有家商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_jiyoujia.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getJiyoujiaItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 今日商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_today.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getTodayItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 精选品牌商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_pinpai.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getPinpaiItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 天猫超市商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_tianmaochaoshi.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getTmallChaoshiItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 预告商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_yugao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getYugaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 店铺商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_dianpu.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getDianpuItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高佣金商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaoyongjin.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaoyongjinItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高销量商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaoxiaoliang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaoxiaoliangItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高评分商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaopingfen.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaopingfenItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 超高券面额商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_gaomiane.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGaomianeItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 偏远地区包邮商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_pybaoyou.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getPybaoyouItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 极品爆单商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_baodan.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getBaodanItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 失效商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/api_shixiao.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getShixiaoItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 热搜词词典 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 热搜词列表响应
     */
    @GetExchange("/api/api_guanjianci.ashx")
    ZhetaokeResponse<List<ZhetaokeHotWord>> getHotWords(
            @RequestParam("appkey") String appkey);

    /**
     * 联想词 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param q      搜索关键词
     * @return 联想词列表响应
     */
    @GetExchange("/api/api_suggest.ashx")
    ZhetaokeResponse<List<ZhetaokeSuggestWord>> getSuggestWords(
            @RequestParam("appkey") String appkey,
            @RequestParam("q") String q);

    /**
     * 轮播图 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 轮播图列表响应
     */
    @GetExchange("/api/api_lunbo.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getLunboItems(
            @RequestParam("appkey") String appkey);

    /**
     * 精选礼物专题 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 礼物商品列表响应
     */
    @GetExchange("/api/api_liwu_zhuanti.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGiftItems(
            @RequestParam("appkey") String appkey);

    /**
     * 淘宝分词 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param q      待分词文本
     * @return 分词结果响应
     */
    @GetExchange("/api/api_fenci.ashx")
    ZhetaokeResponse<List<String>> getFenci(
            @RequestParam("appkey") String appkey,
            @RequestParam("q") String q);

    // ==================== 相似商品 API ====================

    /**
     * 相似商品 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param taoId  商品 ID
     * @return 相似商品列表响应
     */
    @GetExchange("/api/api_item_guess_like.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGuessLikeItems(
            @RequestParam("appkey") String appkey,
            @RequestParam("tao_id") String taoId);

    // ==================== 转链 API ====================

    /**
     * 京东转链 API（新）。
     *
     * @param appkey     折淘客对接秘钥
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

    /**
     * 美团转链 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param link     原始链接
     * @param pid      推广位 ID
     * @param platform 平台类型
     * @return 转链结果
     */
    @GetExchange("/api/open_meituan_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertMeituanLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid,
            @RequestParam(value = "platform", required = false) String platform);

    /**
     * 饿了么转链 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param link     原始链接
     * @param pid      推广位 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_eleme_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertElemeLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid);

    /**
     * 唯品会转链 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param link   原始链接
     * @param pid    推广位 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_vip_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid);

    /**
     * 考拉转链 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param link   原始链接
     * @param pid    推广位 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_kaola_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid);

    /**
     * 拼多多转链 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param link   原始链接
     * @param pid    推广位 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_pdd_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertPddLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid);

    /**
     * 抖音转链 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param link   原始链接
     * @param pid    推广位 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_douyin_promote_link.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("link") String link,
            @RequestParam(value = "pid", required = false) String pid);

    // ==================== 订单查询 API ====================

    /**
     * 京东订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param unionId   京东联盟 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_jd_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryJdOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("unionId") String unionId,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 美团订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_meituan_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 饿了么订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_eleme_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryElemeOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 唯品会订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_vip_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryVipOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 考拉订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_kaola_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryKaolaOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 拼多多订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_pdd_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryPddOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 抖音订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    @GetExchange("/api/open_douyin_order_query.ashx")
    ZhetaokeResponse<List<ZhetaokeOrder>> queryDouyinOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);
}
