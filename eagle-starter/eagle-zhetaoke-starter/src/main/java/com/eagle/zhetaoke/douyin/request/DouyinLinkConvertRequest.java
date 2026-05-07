package com.eagle.zhetaoke.douyin.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class DouyinLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String productUrl;
    private String externalInfo;
    private String needQrCode;
    private String useCoupon;
    private String needShareLink;
}
