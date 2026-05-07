package com.eagle.zhetaoke.jd.client;

import com.eagle.zhetaoke.dto.*;
import com.eagle.zhetaoke.jd.dto.JdOrderDetail;
import com.eagle.zhetaoke.jd.properties.ZhejingkeProperties;
import com.eagle.zhetaoke.jd.request.JdItemFilterRequest;
import com.eagle.zhetaoke.jd.request.JdLinkConvertRequest;
import com.eagle.zhetaoke.jd.request.JdOrderQueryRequest;
import com.eagle.zhetaoke.jd.request.JdSearchRequest;
import com.eagle.zhetaoke.request.LinkConvertRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 折京客 API 高级服务。
 *
 * <p>基于 {@link ZhejingkeClient} 的便捷封装，自动注入 {@code appkey}，
 * 避免每个方法调用都重复传入通用参数。
 *
 * <p><b>使用方式（推荐）：</b>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final ZhejingkeApiService api;
 *
 *     // 方式 1：简单查询（3 个以内参数）
 *     public void demo1() {
 *         var resp = api.getAllItems(1, 20);
 *     }
 *
 *     // 方式 2：复杂查询（使用请求对象）
 *     public void demo2() {
 *         var req = new JdItemFilterRequest();
 *         req.setPage(1);
 *         req.setPageSize(20);
 *         req.setSort("new");
 *         req.setCid(1);
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
public class ZhejingkeApiService {

    private final ZhejingkeClient client;
    private final ZhejingkeProperties properties;

    // ==================== 京东商品查询（简单版本）====================

    /**
     * 全站领券商品查询。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(Integer page, Integer pageSize) {
        return client.getAllItems(properties.getAppkey(), page, pageSize, null, null);
    }

    /**
     * 全站领券商品查询（带筛选条件，请求对象版本）。
     *
     * @param request 筛选查询请求
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getAllItems(JdItemFilterRequest request) {
        return client.getAllItems(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPage(), request.getPageSize(), request.getSort(), request.getCid());
    }

    /**
     * 全网搜索商品。
     *
     * @param q 搜索关键词
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> searchItems(String q) {
        return client.searchItems(properties.getAppkey(), 1, 20, "new", q);
    }

    /**
     * 全网搜索商品（请求对象版本）。
     *
     * @param request 搜索请求
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> searchItems(JdSearchRequest request) {
        return client.searchItems(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPage(), request.getPageSize(), request.getSort(), request.getQ());
    }

    /**
     * 获取商品详情。
     *
     * @param skuId 商品 SKU ID
     * @return 商品详情响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getItemDetail(String skuId) {
        return client.getItemDetail(properties.getAppkey(), skuId);
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
        return client.getHourlyRank(properties.getAppkey(), page, pageSize);
    }

    /**
     * 全天销量榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getDailyRank(Integer page, Integer pageSize) {
        return client.getDailyRank(properties.getAppkey(), page, pageSize);
    }

    /**
     * 实时人气榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getRealTimeRank(Integer page, Integer pageSize) {
        return client.getRealTimeRank(properties.getAppkey(), page, pageSize);
    }

    /**
     * 实时支出佣金榜。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getCommissionRank(Integer page, Integer pageSize) {
        return client.getCommissionRank(properties.getAppkey(), page, pageSize);
    }

    // ==================== 价格商品（简单版本）====================

    /**
     * 9.9 元商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getNineItems(Integer page, Integer pageSize) {
        return client.getNineItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 19.9 元商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getNineteenItems(Integer page, Integer pageSize) {
        return client.getNineteenItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 线报商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getXianbaoItems(Integer page, Integer pageSize) {
        return client.getXianbaoItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 咚咚抢商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getDongdongItems(Integer page, Integer pageSize) {
        return client.getDongdongItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 今日商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getTodayItems(Integer page, Integer pageSize) {
        return client.getTodayItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 预告商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getYugaoItems(Integer page, Integer pageSize) {
        return client.getYugaoItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 超高佣金商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaoyongjinItems(Integer page, Integer pageSize) {
        return client.getGaoyongjinItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 超高销量商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaoxiaoliangItems(Integer page, Integer pageSize) {
        return client.getGaoxiaoliangItems(properties.getAppkey(), page, pageSize);
    }

    /**
     * 超高评分商品。
     *
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 商品列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getGaopingfenItems(Integer page, Integer pageSize) {
        return client.getGaopingfenItems(properties.getAppkey(), page, pageSize);
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
    public ZhetaokeResponse<List<JdOrderDetail>> queryJdOrders(String unionId, String startTime, String endTime,
                                                                     Integer page, Integer pageSize) {
        return client.queryJdOrders(properties.getAppkey(), unionId, startTime, endTime, page, pageSize);
    }

    /**
     * 查询京东订单（请求对象版本）。
     *
     * @param request 订单查询请求
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<JdOrderDetail>> queryJdOrders(JdOrderQueryRequest request) {
        return client.queryJdOrders(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getUnionId(), request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getPageSize());
    }

    /**
     * 查询京东订单（新接口）。
     *
     * @param unionId   京东联盟 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param page      分页页码
     * @param pageSize  每页条数
     * @return 订单列表响应
     */
    public ZhetaokeResponse<List<ZhetaokeOrder>> queryJdOrders2(String unionId, String startTime, String endTime,
                                                                     Integer page, Integer pageSize) {
        return client.queryJdOrders2(properties.getAppkey(), unionId, startTime, endTime, page, pageSize);
    }

    // ==================== 推广位管理 ====================

    /**
     * 创建京东推广位。
     *
     * @param unionId   京东联盟 ID
     * @param key       推广位名称
     * @return 推广位信息
     */
    public ZhetaokeResponse<String> createJdPosition(String unionId, String key) {
        return client.createJdPosition(properties.getAppkey(), unionId, key, null);
    }

    /**
     * 查询京东推广位。
     *
     * @param unionId  京东联盟 ID
     * @param page     分页页码
     * @param pageSize 每页条数
     * @return 推广位列表
     */
    public ZhetaokeResponse<List<String>> queryJdPositions(String unionId, Integer page, Integer pageSize) {
        return client.queryJdPositions(properties.getAppkey(), unionId, page, pageSize);
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
