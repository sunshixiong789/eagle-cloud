package com.eagle.zhetaoke.jd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class JdPyqItemsRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String pageSize;
    private String goodsId;
    private String keyword;
    private String goodsType;
    private String commissionRateStart;
    private String saleNumStart;
    private String price;
    private String couponAmountStart;
    private String sort;
    private String totalCount;
}
