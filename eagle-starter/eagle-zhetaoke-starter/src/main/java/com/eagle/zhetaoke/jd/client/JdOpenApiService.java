package com.eagle.zhetaoke.jd.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.jd.request.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 京东开放平台 API 高级服务。
 *
 * <p>基于 {@link JdOpenClient} 的便捷封装，自动注入 {@code appkey}，
 * 提供简单参数和请求对象双版本调用。
 *
 * @author 孙士雄
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JdOpenApiService {

    private final JdOpenClient client;
    private final ZhetaokeProperties properties;

    // ==================== 转链 ====================

    /**
     * 京东转链（旧版，简单参数）。
     *
     * @param content       京东 url 链接
     * @param jdLianmengId  京东联盟 ID
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertJdLinkOld(String content, String jdLianmengId) {
        return client.convertJdLinkOld(properties.getAppkey(), content, jdLianmengId, null, null);
    }

    /**
     * 京东转链（旧版，请求对象）。
     *
     * @param request 转链请求
     * @return 转链结果
     */
    public ZhetaokeResponse<ZhetaokeLinkResult> convertJdLinkOld(JdLinkConvertRequest request) {
        return client.convertJdLinkOld(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getContent(), request.getJdLianmengId(),
                request.getPositionId(), request.getCouponUrl());
    }

    // ==================== 商品详情 ====================

    /**
     * 京东商品详情[简版]。
     *
     * @param content 京东商品 id 或者京东 url 链接
     * @return 商品详情
     */
    public ZhetaokeResponse<ZhetaokeItem> getJdGoodsDetailSimple(String content) {
        return client.getJdGoodsDetailSimple(properties.getAppkey(), content);
    }

    /**
     * 京东商品详情[详情图]。
     *
     * @param content 京东商品字符串 id 或者京东 url 链接
     * @return 商品大字段详情
     */
    public ZhetaokeResponse<ZhetaokeItem> getJdGoodsBigField(String content) {
        return client.getJdGoodsBigField(properties.getAppkey(), content, null);
    }

    /**
     * 京东商品详情[详版]（请求对象）。
     *
     * @param request 商品详情请求
     * @return 商品详情列表
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJdGoodsDetail(JdGoodsDetailRequest request) {
        return client.getJdGoodsDetail(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getJdAppKey(), request.getJdAppSecret(),
                request.getCid1(), request.getCid2(), request.getCid3(),
                request.getPageIndex(), request.getPageSize(),
                request.getSkuIds(), request.getKeyword(),
                request.getPricefrom(), request.getPriceto(),
                request.getCommissionShareStart(), request.getCommissionShareEnd(),
                request.getOwner(), request.getSortName(), request.getSort(),
                request.getIsCoupon(), request.getIsPG(),
                request.getShopId(), request.getFields(),
                request.getDeliveryType(), request.getArea());
    }

    // ==================== 精选商品 ====================

    /**
     * 京粉精选商品（简单参数）。
     *
     * @param eliteId 频道 ID
     * @return 商品列表
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJingfenGoods(String eliteId) {
        return client.getJingfenGoods(properties.getAppkey(), eliteId,
                null, null, null, null, null, null, null);
    }

    /**
     * 京粉精选商品（请求对象）。
     *
     * @param request 京粉精选请求
     * @return 商品列表
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJingfenGoods(JdJingfenRequest request) {
        return client.getJingfenGoods(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getEliteId(), request.getPageIndex(), request.getPageSize(),
                request.getSortName(), request.getSort(), request.getPid(),
                request.getFields(), request.getForbidTypes());
    }

    // ==================== 朋友圈/礼金商品 ====================

    /**
     * 京东朋友圈火爆商品（请求对象）。
     *
     * @param request 朋友圈商品请求
     * @return 商品列表
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJdPyqItems(JdPyqItemsRequest request) {
        return client.getJdPyqItems(
                request.getPageSize(), request.getGoodsId(), request.getKeyword(),
                request.getGoodsType(), request.getCommissionRateStart(),
                request.getSaleNumStart(), request.getPrice(),
                request.getCouponAmountStart(), request.getSort(), request.getTotalCount());
    }

    /**
     * 京享礼金商品（请求对象）。
     *
     * @param request 礼金商品请求
     * @return 商品列表
     */
    public ZhetaokeResponse<List<ZhetaokeItem>> getJdLiJinItems(JdLiJinRequest request) {
        return client.getJdLiJinItems(
                request.getPageSize(), request.getKeyword(),
                request.getCommissionRateStart(), request.getSaleNumStart(),
                request.getPrice(), request.getCouponAmountStart(),
                request.getSort(), request.getTotalCount());
    }

    // ==================== 私有工具方法 ====================

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
