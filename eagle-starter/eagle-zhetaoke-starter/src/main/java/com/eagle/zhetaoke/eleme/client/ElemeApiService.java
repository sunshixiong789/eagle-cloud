package com.eagle.zhetaoke.eleme.client;

import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.eleme.request.ElemeLinkConvertRequest;
import com.eagle.zhetaoke.eleme.request.ElemeOrderQueryRequest;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElemeApiService {

    private final ElemeClient client;
    private final ZhetaokeProperties properties;

    public ZhetaokeResponse<ZhetaokeLinkResult> generateElemeLink(String sid, String activityId) {
        return client.generateElemeLink(properties.getAppkey(), sid, activityId, null);
    }

    public ZhetaokeResponse<ZhetaokeLinkResult> generateElemeLink(ElemeLinkConvertRequest request) {
        return client.generateElemeLink(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getSid(), request.getActivityId(), request.getCustomerId());
    }

    public ZhetaokeResponse<List<ZhetaokeOrder>> queryElemeOrders2(ElemeOrderQueryRequest request) {
        return client.queryElemeOrders2(
                coalesce(request.getAppkey(), properties.getAppkey()),
                request.getType(), request.getPage(), request.getPageSize(),
                request.getStartTime(), request.getEndTime(),
                request.getOrderId(), request.getSid(), request.getSanPingtaiId());
    }

    private <T> T coalesce(T first, T second) {
        return first != null ? first : second;
    }
}
