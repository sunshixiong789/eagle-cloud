package com.eagle.zhetaoke.pdd.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.pdd.request.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PddApiService {

    private final PddClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> convertPddLink2(PddLinkConvertRequest request) {
        return client.convertPddLink2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getPid(), request.getContent(), request.getCustomParameters());
    }

    public ZhetaokeResponse<ZhetaokeItem> getPddGoodsDetailSimple(PddGoodsDetailRequest request) {
        return client.getPddGoodsDetailSimple(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getPid(), request.getContent());
    }

    public ZhetaokeResponse<String> authorizePddAccount(PddAuthRequest request) {
        return client.authorizePddAccount(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getPid(), request.getCustomParameters(),
                request.getGenerateQqApp(), request.getGenerateWeApp());
    }

    public ZhetaokeResponse<String> queryPddAuthStatus(PddAuthQueryRequest request) {
        return client.queryPddAuthStatus(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getPid(), request.getCustomParameters());
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryPddOrders2(PddOrderQueryRequest request) {
        return client.queryPddOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getStartUpdateTime(), request.getEndUpdateTime(),
                request.getQueryOrderType(), request.getPage(),
                request.getPageSize(), request.getCashGiftOrder());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> searchPddGoodsDetail(PddGoodsSearchRequest request) {
        return client.searchPddGoodsDetail(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getPid(), request.getCustomParameters(),
                request.getKeyword(), request.getCatId(),
                request.getActivityTags(), request.getBlockCatPackages(),
                request.getBlockCats(), request.getIsBrandGoods(),
                request.getMerchantType(), request.getSortType(),
                request.getUseCustomized(), request.getWithCoupon());
    }

    public ZhetaokeResponse<List<String>> getPddGoodsCats(PddGoodsCatsRequest request) {
        return client.getPddGoodsCats(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPddAppKey(), request.getPddAppSecret(),
                request.getParentCatId());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
