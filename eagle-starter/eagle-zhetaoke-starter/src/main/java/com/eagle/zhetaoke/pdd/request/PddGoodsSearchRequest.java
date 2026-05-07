package com.eagle.zhetaoke.pdd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class PddGoodsSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String pddAppKey;
    private String pddAppSecret;
    private String pid;
    private String customParameters;
    private String keyword;
    private String catId;
    private String activityTags;
    private String blockCatPackages;
    private String blockCats;
    private String isBrandGoods;
    private String merchantType;
    private String sortType;
    private String useCustomized;
    private String withCoupon;
}
