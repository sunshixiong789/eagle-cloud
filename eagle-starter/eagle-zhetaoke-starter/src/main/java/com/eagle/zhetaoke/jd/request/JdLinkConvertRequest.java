package com.eagle.zhetaoke.jd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class JdLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String content;
    private String jdLianmengId;
    private String positionId;
    private String couponUrl;
}
