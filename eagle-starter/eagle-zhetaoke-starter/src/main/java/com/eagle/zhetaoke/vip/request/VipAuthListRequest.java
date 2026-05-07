package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipAuthListRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String page;
    private String expireDay;
    private String sid;
}
