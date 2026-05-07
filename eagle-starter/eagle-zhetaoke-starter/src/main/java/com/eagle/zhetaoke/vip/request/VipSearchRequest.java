package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String keyword;
    private String fieldName;
    private String order;
    private String page;
    private String pageSize;
    private String priceStart;
    private String priceEnd;
}
