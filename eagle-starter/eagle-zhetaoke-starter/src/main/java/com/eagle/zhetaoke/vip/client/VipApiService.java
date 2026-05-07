package com.eagle.zhetaoke.vip.client;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import com.eagle.zhetaoke.vip.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipApiService {

    private final VipClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(String sid, String url) {
        return client.convertVipLink(properties.getAppkey(), sid, url, null, null);
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> convertVipLink(VipLinkConvertRequest request) {
        return client.convertVipLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getUrl(),
                request.getChanTag(), request.getStatParam());
    }

    public ZhetaokeResponse<String> authorizeVipAccount() {
        return client.authorizeVipAccount(properties.getAppkey());
    }

    public ZhetaokeResponse<List<String>> getVipAuthorizationList(VipAuthListRequest request) {
        return client.getVipAuthorizationList(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPage(), request.getExpireDay(), request.getSid());
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryVipOrders2(VipOrderQueryRequest request) {
        return client.queryVipOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getStatus(),
                request.getOrderTimeStart(), request.getOrderTimeEnd(),
                request.getPage(), request.getPageSize(),
                request.getUpdateTimeStart(), request.getUpdateTimeEnd(),
                request.getOrderSnList(), request.getVendorCode(), request.getChanTag());
    }

    public ZhetaokeResponse<ZhetaokeItem> getVipGoodsDetail(String sid, String id) {
        return client.getVipGoodsDetail(properties.getAppkey(), sid, id);
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> getVipWenanList(VipWenanRequest request) {
        return client.getVipWenanList(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getPage(), request.getPageSize(),
                request.getSort(), request.getTotalCount());
    }

    public ZhetaokeResponse<List<ZhetaokeItem>> searchVipGoods(VipSearchRequest request) {
        return client.searchVipGoods(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getKeyword(),
                request.getFieldName(), request.getOrder(),
                request.getPage(), request.getPageSize(),
                request.getPriceStart(), request.getPriceEnd());
    }

    public ZhetaokeResponse<ZhetaokeItem> getVipGoodsDetailV2(VipGoodsDetailV2Request request) {
        return client.getVipGoodsDetailV2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getId(),
                request.getQueryDetail(), request.getQueryStock(),
                request.getQueryReputation(), request.getQueryStoreServiceCapability(),
                request.getQueryPMSAct(), request.getExtendBySpu(),
                request.getQueryExclusiveCoupon(), request.getExtendSku());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
