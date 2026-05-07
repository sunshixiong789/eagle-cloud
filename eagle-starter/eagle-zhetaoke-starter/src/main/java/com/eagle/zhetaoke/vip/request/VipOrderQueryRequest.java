package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipOrderQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String status;
    private String orderTimeStart;
    private String orderTimeEnd;
    private String page;
    private String pageSize;
    private String updateTimeStart;
    private String updateTimeEnd;
    private String orderSnList;
    private String vendorCode;
    private String chanTag;
}
