package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String url;
    private String chanTag;
    private String statParam;
}
