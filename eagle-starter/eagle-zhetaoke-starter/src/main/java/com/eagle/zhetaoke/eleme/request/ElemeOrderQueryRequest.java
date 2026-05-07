package com.eagle.zhetaoke.eleme.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class ElemeOrderQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String type;
    private String page;
    private String pageSize;
    private String startTime;
    private String endTime;
    private String orderId;
    private String sid;
    private String sanPingtaiId;
}
