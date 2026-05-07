package com.eagle.zhetaoke.meituan.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class MeituanOrderQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String type;
    private String page;
    private String pageSize;
    private String startTime;
    private String endTime;
    private String orderId;
    private String sid;
}
