package com.eagle.zhetaoke.douyin.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class DouyinProductDetailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String productIds;
    private String fields;
}
