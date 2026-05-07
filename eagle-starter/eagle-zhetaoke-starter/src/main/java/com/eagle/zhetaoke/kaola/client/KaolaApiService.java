package com.eagle.zhetaoke.kaola.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.kaola.request.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KaolaApiService {

    private final KaolaClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink2(String sid, String targetUrl) {
        return client.convertKaolaLink2(properties.getAppkey(), sid, targetUrl, null);
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> convertKaolaLink2(KaolaLinkConvertRequest request) {
        return client.convertKaolaLink2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getTargetUrl(), request.getTrackingCode2());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> getKaolaSelectedGoods(KaolaSelectedGoodsRequest request) {
        return client.getKaolaSelectedGoods(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPoolName(), request.getPageNo(), request.getPageSize());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> searchKaolaGoods(KaolaSearchRequest request) {
        return client.searchKaolaGoods(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getKeyWord(), request.getType(), request.getDesc(),
                request.getPageNo(), request.getPageSize());
    }

    public ZhetaokeResponse<ZhetaokeItem> getKaolaGoodsDetail(String goodsIds) {
        return client.getKaolaGoodsDetail(properties.getAppkey(), goodsIds);
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryKaolaOrders2(KaolaOrderQueryRequest request) {
        return client.queryKaolaOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getType(), request.getPage(), request.getPageSize(),
                request.getStartTime(), request.getEndTime(),
                request.getOrderId(), request.getSid(), request.getSanPingtaiId());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
