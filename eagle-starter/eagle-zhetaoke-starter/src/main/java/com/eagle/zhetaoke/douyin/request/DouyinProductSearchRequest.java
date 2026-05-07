package com.eagle.zhetaoke.douyin.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class DouyinProductSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String title;
    private String firstCids;
    private String secondCids;
    private String thirdCids;
    private String priceMin;
    private String priceMax;
    private String sellNumMin;
    private String sellNumMax;
    private String searchType;
    private String sortType;
    private String cosFeeMin;
    private String cosFeeMax;
    private String cosRatioMin;
    private String cosRatioMax;
    private String page;
    private String pageSize;
    private String shareStatus;
    private String tag;
}
