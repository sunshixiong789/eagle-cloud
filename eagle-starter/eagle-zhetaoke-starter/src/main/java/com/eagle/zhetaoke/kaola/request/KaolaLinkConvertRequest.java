package com.eagle.zhetaoke.kaola.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class KaolaLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String targetUrl;
    private String trackingCode2;
}
