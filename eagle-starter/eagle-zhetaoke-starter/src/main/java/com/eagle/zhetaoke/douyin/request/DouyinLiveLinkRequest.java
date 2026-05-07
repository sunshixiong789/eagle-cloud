package com.eagle.zhetaoke.douyin.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class DouyinLiveLinkRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String buyinId;
    private String dyCode;
    private String externalInfo;
}
