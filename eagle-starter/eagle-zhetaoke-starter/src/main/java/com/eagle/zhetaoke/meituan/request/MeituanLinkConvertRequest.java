package com.eagle.zhetaoke.meituan.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class MeituanLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String actId;
    private String linkType;
    private String miniCode;
    private String miniCode2;
    private String miniCode3;
    private String platform;
    private String customerId;
}
