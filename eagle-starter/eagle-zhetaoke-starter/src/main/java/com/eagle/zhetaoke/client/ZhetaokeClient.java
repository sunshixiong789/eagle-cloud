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

    // ==================== 淘宝订单中心 API ====================

    /**
     * 新订单查询 API（淘宝联盟）。
     *
     * @param appkey       折淘客对接秘钥
     * @param sid          淘客账号授权 ID
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param queryType    查询时间类型
     * @param positionIndex 位点
     * @param pageSize     页大小
     * @param memberType   推广者角色类型
     * @param tkStatus     淘客订单状态
     * @param jumpType     跳转类型
     * @param pageNo       第几页
     * @param orderScene   场景订单场景类型
     * @param signurl      返回值类型
     * @return 淘宝订单列表响应
     */
    @GetExchange("/api/open_dingdanchaxun2.ashx")
    ZhetaokeResponse<List<TbOrderDetail>> queryTbOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("start_time") String startTime,
            @RequestParam("end_time") String endTime,
            @RequestParam(value = "query_type", required = false) String queryType,
            @RequestParam(value = "position_index", required = false) String positionIndex,
            @RequestParam(value = "page_size", required = false) String pageSize,
            @RequestParam(value = "member_type", required = false) String memberType,
            @RequestParam(value = "tk_status", required = false) String tkStatus,
            @RequestParam(value = "jump_type", required = false) String jumpType,
            @RequestParam(value = "page_no", required = false) String pageNo,
            @RequestParam(value = "order_scene", required = false) String orderScene,
            @RequestParam(value = "signurl", required = false) Integer signurl);

    /**
     * 淘宝维权订单查询 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param sid       淘客账号授权 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageSize  页大小
     * @param pageNo    第几页
     * @param refundType 维权类型
     * @param bizType   业务类型
     * @param simplify  是否简化
     * @return 淘宝维权订单列表响应
     */
    @GetExchange("/Api/open_dingdanchaxun2_refund.ashx")
    ZhetaokeResponse<List<TbOrderDetail>> queryTbRefundOrders(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("start_time") String startTime,
            @RequestParam("end_time") String endTime,
            @RequestParam(value = "page_size", required = false) String pageSize,
            @RequestParam(value = "page_no", required = false) String pageNo,
            @RequestParam(value = "refund_type", required = false) String refundType,
            @RequestParam(value = "biz_type", required = false) String bizType,
            @RequestParam(value = "simplify", required = false) String simplify);

    /**
     * 高佣转链 API（商品 ID）。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        淘客账号授权 ID
     * @param pid        淘客 PID
     * @param numIid     商品 ID
     * @param relationId 渠道关系 ID
     * @param specialId  会员运营 ID
     * @param signurl    返回值类型
     * @return 转链结果
     */
    @GetExchange("/api/open_gaoyongzhuanlian.ashx")
    ZhetaokeResponse<ZhetaokeItem> convertHighCommission(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("num_iid") String numIid,
            @RequestParam(value = "relation_id", required = false) String relationId,
            @RequestParam(value = "special_id", required = false) String specialId,
            @RequestParam(value = "signurl", required = false) Integer signurl);

    /**
     * 高佣转链 API（淘口令）。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        淘客账号授权 ID
     * @param pid        淘客 PID
     * @param tkl        淘口令
     * @param relationId 渠道关系 ID
     * @param specialId  会员运营 ID
     * @param signurl    返回值类型
     * @return 转链结果
     */
    @GetExchange("/api/open_gaoyongzhuanlian_tkl.ashx")
    ZhetaokeResponse<ZhetaokeItem> convertHighCommissionByTkl(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("tkl") String tkl,
            @RequestParam(value = "relation_id", required = false) String relationId,
            @RequestParam(value = "special_id", required = false) String specialId,
            @RequestParam(value = "signurl", required = false) Integer signurl);

    /**
     * 批量高佣转链 API（商品 ID）。
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     淘客账号授权 ID
     * @param pid     淘客 PID
     * @param numIids 多个商品 ID，逗号分隔
     * @param signurl 返回值类型
     * @return 转链结果列表
     */
    @GetExchange("/api/open_gaoyongzhuanlian_piliang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> batchConvertHighCommission(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("num_iids") String numIids,
            @RequestParam(value = "signurl", required = false) Integer signurl);

    /**
     * 批量高佣转链 API（淘口令）。
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     淘客账号授权 ID
     * @param pid     淘客 PID
     * @param tkls    多个淘口令，逗号分隔
     * @param signurl 返回值类型
     * @return 转链结果列表
     */
    @GetExchange("/api/open_gaoyongzhuanlian_tkl_piliang.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> batchConvertHighCommissionByTkl(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("tkls") String tkls,
            @RequestParam(value = "signurl", required = false) Integer signurl);

    /**
     * 淘口令创建 API。
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     淘客账号授权 ID
     * @param text    口令弹框内容
     * @param url     口令跳转目标页 URL
     * @param logo    口令弹框 logo URL
     * @param signurl 返回值类型
     * @param type    结果类型
     * @return 淘口令结果
     */
    @GetExchange("/api/open_tkl_create.ashx")
    ZhetaokeResponse<TklResult> createTkl(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam("url") String url,
            @RequestParam(value = "logo", required = false) String logo,
            @RequestParam(value = "signurl", required = false) Integer signurl,
            @RequestParam(value = "type", required = false) String type);

    /**
     * 解析商品编号 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param url    商品链接或淘口令
     * @return 商品编号信息
     */
    @GetExchange("/api/open_shangpin_id.ashx")
    ZhetaokeResponse<ZhetaokeItem> parseItemId(
            @RequestParam("appkey") String appkey,
            @RequestParam("url") String url);

    /**
     * 淘宝短链接转换 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param url    原始链接
     * @return 短链接结果
     */
    @GetExchange("/api/open_shorturl_taobao_get.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertShortUrl(
            @RequestParam("appkey") String appkey,
            @RequestParam("url") String url);

    /**
     * 店铺链接转换 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param sid    淘客账号授权 ID
     * @param pid    淘客 PID
     * @param url    店铺链接
     * @return 转链结果
     */
    @GetExchange("/api/open_shop_convert.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertShopLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("url") String url);

    /**
     * 淘礼金创建 API。
     *
     * @param appkey    折淘客对接秘钥
     * @param sid       淘客账号授权 ID
     * @param itemId    商品 ID
     * @param totalNum  淘礼金总个数
     * @param totalAmount 淘礼金总面额
     * @param name      淘礼金名称
     * @param perFace   单个淘礼金面额
     * @param winNum    中奖概率
     * @return 淘礼金结果
     */
    @GetExchange("/api/open_taolijin2_create.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> createTaoLiJin(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("item_id") String itemId,
            @RequestParam("total_num") String totalNum,
            @RequestParam("total_amount") String totalAmount,
            @RequestParam("name") String name,
            @RequestParam(value = "per_face", required = false) String perFace,
            @RequestParam(value = "win_num", required = false) String winNum);

    /**
     * 渠道邀请码生成 API。
     *
     * @param appkey 折淘客对接秘钥
     * @param sid    淘客账号授权 ID
     * @return 邀请码结果
     */
    @GetExchange("/api/open_sc_invitecode_get.ashx")
    ZhetaokeResponse<PublisherInfo> getInviteCode(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid);

    /**
     * 渠道备案 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        淘客账号授权 ID
     * @param relationId 渠道关系 ID
     * @param specialId  会员运营 ID
     * @param accountName 账户名称
     * @param accountType 账户类型
     * @param inviteCode  邀请码
     * @param remark     备注
     * @param infoType   信息类型
     * @return 备案结果
     */
    @GetExchange("/api/open_sc_publisher_save.ashx")
    ZhetaokeResponse<PublisherInfo> savePublisher(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "relation_id", required = false) String relationId,
            @RequestParam(value = "special_id", required = false) String specialId,
            @RequestParam(value = "account_name", required = false) String accountName,
            @RequestParam(value = "account_type", required = false) String accountType,
            @RequestParam(value = "invite_code", required = false) String inviteCode,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "info_type", required = false) String infoType);

    /**
     * 渠道信息查询 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        淘客账号授权 ID
     * @param relationId 渠道关系 ID
     * @param specialId  会员运营 ID
     * @param infoType   信息类型
     * @return 渠道信息列表
     */
    @GetExchange("/api/open_sc_publisher_get.ashx")
    ZhetaokeResponse<List<PublisherInfo>> getPublisherInfo(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam(value = "relation_id", required = false) String relationId,
            @RequestParam(value = "special_id", required = false) String specialId,
            @RequestParam(value = "info_type", required = false) String infoType);

    /**
     * 接口调用日志 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 日志列表
     */
    @GetExchange("/api/open_log.ashx")
    ZhetaokeResponse<List<String>> getApiLogs(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("start_time") String startTime,
            @RequestParam("end_time") String endTime,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 批量全网商品详情（简版）API。
     *
     * @param appkey  折淘客对接秘钥
     * @param sid     淘客账号授权 ID
     * @param pid     淘客 PID
     * @param numIids 多个商品 ID，逗号分隔
     * @return 商品详情列表
     */
    @GetExchange("/api/open_item_info.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getItemInfoBatch(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("num_iids") String numIids);

    /**
     * 淘宝联盟猜你喜欢商品 API。
     *
     * @param appkey   折淘客对接秘钥
     * @param sid      淘客账号授权 ID
     * @param pid      淘客 PID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    @GetExchange("/api/open_item_guess_like2.ashx")
    ZhetaokeResponse<List<ZhetaokeItem>> getGuessLikeItems2(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "page_size", required = false) Integer pageSize);

    /**
     * 淘宝联盟官方活动转链 API。
     *
     * @param appkey     折淘客对接秘钥
     * @param sid        淘客账号授权 ID
     * @param pid        淘客 PID
     * @param activityId 官方活动 ID
     * @param adzoneId   推广位 ID
     * @param relationId 渠道关系 ID
     * @param specialId  会员运营 ID
     * @param unionId    会场 ID
     * @return 转链结果
     */
    @GetExchange("/api/open_activitylink_get.ashx")
    ZhetaokeResponse<ZhetaokeLinkResult> convertActivityLink(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("pid") String pid,
            @RequestParam("activity_id") String activityId,
            @RequestParam(value = "adzone_id", required = false) String adzoneId,
            @RequestParam(value = "relation_id", required = false) String relationId,
            @RequestParam(value = "special_id", required = false) String specialId,
            @RequestParam(value = "union_id", required = false) String unionId);

    /**
     * 淘宝联盟官方活动列表 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 活动列表响应
     */
    @GetExchange("/api/open_activity_list.ashx")
    ZhetaokeResponse<List<TbActivity>> getActivityList(
            @RequestParam("appkey") String appkey);

    /**
     * 创建推广位 API。
     *
     * @param appkey      折淘客对接秘钥
     * @param sid         淘客账号授权 ID
     * @param adzoneName  推广位名称
     * @param siteId      站点 ID
     * @param mediaType   媒体类型
     * @return 推广位信息
     */
    @GetExchange("/api/open_create_pid2.ashx")
    ZhetaokeResponse<AdzoneInfo> createPid(
            @RequestParam("appkey") String appkey,
            @RequestParam("sid") String sid,
            @RequestParam("adzone_name") String adzoneName,
            @RequestParam(value = "site_id", required = false) String siteId,
            @RequestParam(value = "media_type", required = false) String mediaType);

    /**
     * 淘客账号授权 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 授权结果
     */
    @GetExchange("/api/open_taokeshouquan.ashx")
    ZhetaokeResponse<String> authorizeTaokeAccount(
            @RequestParam("appkey") String appkey);

    /**
     * 获取账户授权列表 API。
     *
     * @param appkey 折淘客对接秘钥
     * @return 授权列表
     */
    @GetExchange("/api/open_taokeshouquaninfo.ashx")
    ZhetaokeResponse<List<String>> getAuthorizationList(
            @RequestParam("appkey") String appkey);
}
