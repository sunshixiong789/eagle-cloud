package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipGoodsDetailV2Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String id;
    private String queryDetail;
    private String queryStock;
    private String queryReputation;
    private String queryStoreServiceCapability;
    private String queryPMSAct;
    private String extendBySpu;
    private String queryExclusiveCoupon;
    private String extendSku;
}
