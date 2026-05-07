package com.eagle.zhetaoke.douyin.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.douyin.request.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinApiService {

    private final DouyinClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink2(String sid, String productUrl) {
        return client.convertDouyinLink2(properties.getAppkey(), sid, productUrl, null, null, null, null);
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLink2(DouyinLinkConvertRequest request) {
        return client.convertDouyinLink2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getProductUrl(),
                request.getExternalInfo(), request.getNeedQrCode(),
                request.getUseCoupon(), request.getNeedShareLink());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> getDouyinProductDetail(DouyinProductDetailRequest request) {
        return client.getDouyinProductDetail(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getProductIds(), request.getFields());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> searchDouyinProducts(DouyinProductSearchRequest request) {
        return client.searchDouyinProducts(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getTitle(),
                request.getFirstCids(), request.getSecondCids(), request.getThirdCids(),
                request.getPriceMin(), request.getPriceMax(),
                request.getSellNumMin(), request.getSellNumMax(),
                request.getSearchType(), request.getSortType(),
                request.getCosFeeMin(), request.getCosFeeMax(),
                request.getCosRatioMin(), request.getCosRatioMax(),
                request.getPage(), request.getPageSize(),
                request.getShareStatus(), request.getTag());
    }

    public ZhetaokeResponse<ZhetaokeItem> parseDouyinCommand(String sid, String command) {
        return client.parseDouyinCommand(properties.getAppkey(), sid, command);
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryDouyinOrders2(DouyinOrderQueryRequest request) {
        return client.queryDouyinOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getType(), request.getPage(), request.getPageSize(),
                request.getStartTime(), request.getEndTime(),
                request.getOrderId(), request.getSid(), request.getSanPingtaiId());
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> convertDouyinLiveLink(DouyinLiveLinkRequest request) {
        return client.convertDouyinLiveLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getBuyinId(),
                request.getDyCode(), request.getExternalInfo());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
