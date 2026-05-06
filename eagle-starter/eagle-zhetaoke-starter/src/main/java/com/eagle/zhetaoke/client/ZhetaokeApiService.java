package com.eagle.zhetaoke.client;

import com.eagle.zhetaoke.dto.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import com.eagle.zhetaoke.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 折淘客 API 高级服务。
 *
 * <p>基于 {@link ZhetaokeClient} 的便捷封装，自动注入 {@code appkey}、{@code sid}、{@code pid}，
 * 避免每个方法调用都重复传入通用参数。
 *
 * <p><b>使用方式（推荐）：</b>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final ZhetaokeApiService api;
 *
 *     // 方式 1：简单查询（3 个以内参数）
 *     public void demo1() {
 *         var resp = api.getAllItems(1, 20, "new");
 *     }
 *
 *     // 方式 2：复杂查询（使用请求对象）
 *     public void demo2() {
 *         var req = new ItemFilterRequest();
 *         req.setPage(1);
 *         req.setPageSize(20);
 *         req.setSort("new");
 *         req.setCid(1);        // 女装
 *         req.setTj("tmall");   // 天猫
 *         var resp = api.getAllItems(req);
 *     }
 * }
 * }</pre>
 *
 * @author 孙士雄
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhetaokeApiService {

    private final ZhetaokeClient client;
    private final ZhetaokeProperties properties;

    // ==================== 淘宝商品查询（简单版本）====================

    /**
     * 全站领券商品查询。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @param sort     排序方式
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(Integer page, Integer pageSize, String sort) {
        return client.getAllItems(properties.getAppkey(), properties.getSid(), properties.getPid(),
                page, pageSize, sort, null, null, null, null);
    }

    /**
     * 全站领券商品查询（带筛选条件，请求对象版本）。
     *
     * @param request 筛选查询请求
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(ItemFilterRequest request) {
        return client.getAllItems(
                coalesce(request.getAppkey(), properties.getAppkey()),
                coalesce(request.getSid(), properties.getSid()),
                coalesce(request.getPid(), properties.getPid()),
                request.getPage(), request.getPageSize(), request.getSort(),
                request.getCid(), request.getTj(), request.getJt(), request.getJh());
    }

    /**
     * 全网搜索商品。
     *
     * @param q 搜索关键词
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> searchItems(String q) {
        return client.searchItems(properties.getAppkey(), properties.getSid(), properties.getPid(),
                1, 20, "new", q);
    }

    /**
     * 全网搜索商品（请求对象版本）。
     *
     * @param request 搜索请求
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> searchItems(SearchRequest request) {
        return client.searchItems(
                coalesce(request.getAppkey(), properties.getAppkey()),
                coalesce(request.getSid(), properties.getSid()),
                coalesce(request.getPid(), properties.getPid()),
                request.getPage(), request.getPageSize(), request.getSort(), request.getQ());
    }

    /**
     * 获取商品详情。
     *
     * @param taoId 商品 ID
     * @return 商品详情响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getItemDetail(String taoId) {
        return client.getItemDetail(properties.getAppkey(), properties.getSid(), properties.getPid(),
                taoId, null, null);
    }

    /**
     * 获取商品详情（请求对象版本）。
     *
     * @param request 商品详情请求
     * @return 商品详情响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getItemDetail(ItemDetailRequest request) {
        return client.getItemDetail(
                coalesce(request.getAppkey(), properties.getAppkey()),
                coalesce(request.getSid(), properties.getSid()),
                coalesce(request.getPid(), properties.getPid()),
                request.getTaoId(), request.getCode(), request.getNumIids());
    }

    // ==================== 销量榜（简单版本）====================

    /**
     * 两小时销量榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getHourlyRank(Integer page, Integer pageSize) {
        return client.getHourlyRank(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 全天销量榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getDailyRank(Integer page, Integer pageSize) {
        return client.getDailyRank(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 实时人气榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getRealTimeRank(Integer page, Integer pageSize) {
        return client.getRealTimeRank(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 实时支出佣金榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getCommissionRank(Integer page, Integer pageSize) {
        return client.getCommissionRank(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    // ==================== 价格/分类商品（简单版本）====================

    /**
     * 9.9 元商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getNineItems(Integer page, Integer pageSize) {
        return client.getNineItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 19.9 元商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getNineteenItems(Integer page, Integer pageSize) {
        return client.getNineteenItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 视频（抖货）商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getVideoItems(Integer page, Integer pageSize) {
        return client.getVideoItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 线报商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getXianbaoItems(Integer page, Integer pageSize) {
        return client.getXianbaoItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 咚咚抢商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getDongdongItems(Integer page, Integer pageSize) {
        return client.getDongdongItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 朋友圈火爆商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getPyqItems(Integer page, Integer pageSize) {
        return client.getPyqItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 天猫商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getTmallItems(Integer page, Integer pageSize) {
        return client.getTmallItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 金牌卖家商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGoldItems(Integer page, Integer pageSize) {
        return client.getGoldItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 淘抢购商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getTaoqianggouItems(Integer page, Integer pageSize) {
        return client.getTaoqianggouItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 聚划算商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJuhuasuanItems(Integer page, Integer pageSize) {
        return client.getJuhuasuanItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 海淘商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getHaitaoItems(Integer page, Integer pageSize) {
        return client.getHaitaoItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 极有家商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJiyoujiaItems(Integer page, Integer pageSize) {
        return client.getJiyoujiaItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 今日商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getTodayItems(Integer page, Integer pageSize) {
        return client.getTodayItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 精选品牌商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getPinpaiItems(Integer page, Integer pageSize) {
        return client.getPinpaiItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 天猫超市商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getTmallChaoshiItems(Integer page, Integer pageSize) {
        return client.getTmallChaoshiItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 预告商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getYugaoItems(Integer page, Integer pageSize) {
        return client.getYugaoItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 店铺商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getDianpuItems(Integer page, Integer pageSize) {
        return client.getDianpuItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 超高佣金商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaoyongjinItems(Integer page, Integer pageSize) {
        return client.getGaoyongjinItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 超高销量商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaoxiaoliangItems(Integer page, Integer pageSize) {
        return client.getGaoxiaoliangItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 超高评分商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaopingfenItems(Integer page, Integer pageSize) {
        return client.getGaopingfenItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 超高券面额商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaomianeItems(Integer page, Integer pageSize) {
        return client.getGaomianeItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 偏远地区包邮商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getPybaoyouItems(Integer page, Integer pageSize) {
        return client.getPybaoyouItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 极品爆单商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getBaodanItems(Integer page, Integer pageSize) {
        return client.getBaodanItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    /**
     * 失效商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getShixiaoItems(Integer page, Integer pageSize) {
        return client.getShixiaoItems(properties.getAppkey(), properties.getSid(), properties.getPid(), page, pageSize);
    }

    // ==================== 辅助工具 API（简单版本）====================

    /**
     * 获取热搜词词典。
     *
     * @return 热搜词列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeHotWord>> getHotWords() {
        return client.getHotWords(properties.getAppkey());
    }

    /**
     * 获取联想词。
     *
     * @param q 搜索关键词
     * @return 联想词列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeSuggestWord>> getSuggestWords(String q) {
        return client.getSuggestWords(properties.getAppkey(), q);
    }

    /**
     * 获取轮播图。
     *
     * @return 轮播图列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getLunboItems() {
        return client.getLunboItems(properties.getAppkey());
    }

    /**
     * 获取精选礼物专题。
     *
     * @return 礼物商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGiftItems() {
        return client.getGiftItems(properties.getAppkey());
    }

    /**
     * 淘宝分词。
     *
     * @param q 待分词文本
     * @return 分词结果响应
     */
    public ZhetaokeResponse<List<String>> getFenci(String q) {
        return client.getFenci(properties.getAppkey(), q);
    }

    /**
     * 获取相似商品。
     *
     * @param taoId 商品 ID
     * @return 相似商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGuessLikeItems(String taoId) {
        return client.getGuessLikeItems(properties.getAppkey(), taoId);
    }

    // ==================== 转链（简单版本）====================

    /**
     * 京东转链。
     *
     * @param materialId 推广物料 URL
     * @param unionId    京东联盟 ID
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertJdLink(String materialId, String unionId) {
        return client.convertJdLink(properties.getAppkey(), materialId, unionId, null, "2");
    }

    /**
     * 京东转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertJdLink(LinkConvertRequest request) {
        return client.convertJdLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getMaterialId(), request.getUnionId(),
                request.getPositionId(), request.getChainType());
    }

    /**
     * 美团转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertMeituanLink(String link) {
        return client.convertMeituanLink(properties.getAppkey(), link, null, null);
    }

    /**
     * 美团转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertMeituanLink(LinkConvertRequest request) {
        return client.convertMeituanLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid(), request.getPlatform());
    }

    /**
     * 饿了么转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertElemeLink(String link) {
        return client.convertElemeLink(properties.getAppkey(), link, null);
    }

    /**
     * 饿了么转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertElemeLink(LinkConvertRequest request) {
        return client.convertElemeLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid());
    }

    /**
     * 唯品会转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(String link) {
        return client.convertVipLink(properties.getAppkey(), link, null);
    }

    /**
     * 唯品会转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(LinkConvertRequest request) {
        return client.convertVipLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid());
    }

    /**
     * 考拉转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink(String link) {
        return client.convertKaolaLink(properties.getAppkey(), link, null);
    }

    /**
     * 考拉转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink(LinkConvertRequest request) {
        return client.convertKaolaLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid());
    }

    /**
     * 拼多多转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertPddLink(String link) {
        return client.convertPddLink(properties.getAppkey(), link, null);
    }

    /**
     * 拼多多转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertPddLink(LinkConvertRequest request) {
        return client.convertPddLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid());
    }

    /**
     * 抖音转链。
     *
     * @param link 原始链接
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink(String link) {
        return client.convertDouyinLink(properties.getAppkey(), link, null);
    }

    /**
     * 抖音转链（请求对象版本）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink(LinkConvertRequest request) {
        return client.convertDouyinLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getLink(), request.getPid());
    }

    // ==================== 订单查询（简单版本）====================

    /**
     * 查询京东订单。
     *
     * @param unionId   京东联盟 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryJdOrders(String unionId, String startTime, String endTime,
                                                                 Integer page, Integer pageSize) {
        return client.queryJdOrders(properties.getAppkey(), unionId, startTime, endTime, page, pageSize);
    }

    /**
     * 查询京东订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryJdOrders(OrderQueryRequest request) {
        return client.queryJdOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getUnionId(), request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询美团订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders(String startTime, String endTime,
                                                                     Integer page, Integer pageSize) {
        return client.queryMeituanOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询美团订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders(OrderQueryRequest request) {
        return client.queryMeituanOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询饿了么订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryElemeOrders(String startTime, String endTime,
                                                                    Integer page, Integer pageSize) {
        return client.queryElemeOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询饿了么订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryElemeOrders(OrderQueryRequest request) {
        return client.queryElemeOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询唯品会订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryVipOrders(String startTime, String endTime,
                                                                  Integer page, Integer pageSize) {
        return client.queryVipOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询唯品会订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryVipOrders(OrderQueryRequest request) {
        return client.queryVipOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询考拉订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryKaolaOrders(String startTime, String endTime,
                                                                   Integer page, Integer pageSize) {
        return client.queryKaolaOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询考拉订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryKaolaOrders(OrderQueryRequest request) {
        return client.queryKaolaOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询拼多多订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryPddOrders(String startTime, String endTime,
                                                                  Integer page, Integer pageSize) {
        return client.queryPddOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询拼多多订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryPddOrders(OrderQueryRequest request) {
        return client.queryPddOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询抖音订单。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryDouyinOrders(String startTime, String endTime,
                                                                    Integer page, Integer pageSize) {
        return client.queryDouyinOrders(properties.getAppkey(), startTime, endTime, page, pageSize);
    }

    /**
     * 查询抖音订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryDouyinOrders(OrderQueryRequest request) {
        return client.queryDouyinOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    // ==================== 私有工具方法 ====================

    /**
     * 返回第一个非 null 的值。
     *
     * @param first  优先值
     * @param second 默认值
     * @param <T>    值类型
     * @return 非 null 值
     */
    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
