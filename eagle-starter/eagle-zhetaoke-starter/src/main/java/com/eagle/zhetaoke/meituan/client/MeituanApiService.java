package com.eagle.zhetaoke.meituan.client;

import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.meituan.request.MeituanLinkConvertRequest;
import com.eagle.zhetaoke.meituan.request.MeituanOrderQueryRequest;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeituanApiService {

    private final MeituanClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> generateMeituanLink(String sid, String actId, String linkType) {
        return client.generateMeituanLink(properties.getAppkey(), sid, actId, linkType, null, null, null, null, null);
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> generateMeituanLink(MeituanLinkConvertRequest request) {
        return client.generateMeituanLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getActId(), request.getLinkType(),
                request.getMiniCode(), request.getMiniCode2(), request.getMiniCode3(),
                request.getPlatform(), request.getCustomerId());
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders2(String type, String page, String pageSize) {
        return client.queryMeituanOrders2(properties.getAppkey(), type, page, pageSize, null, null, null, null);
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryMeituanOrders2(MeituanOrderQueryRequest request) {
        return client.queryMeituanOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getType(), request.getPage(), request.getPageSize(),
                request.getStartTime(), request.getEndTime(),
                request.getOrderId(), request.getSid());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
