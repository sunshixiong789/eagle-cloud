package com.eagle.zhetaoke.jd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class JdGoodsDetailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String jdAppKey;
    private String jdAppSecret;
    private String cid1;
    private String cid2;
    private String cid3;
    private String pageIndex;
    private String pageSize;
    private String skuIds;
    private String keyword;
    private String pricefrom;
    private String priceto;
    private String commissionShareStart;
    private String commissionShareEnd;
    private String owner;
    private String sortName;
    private String sort;
    private String isCoupon;
    private String isPG;
    private String shopId;
    private String fields;
    private String deliveryType;
    private String area;
}
