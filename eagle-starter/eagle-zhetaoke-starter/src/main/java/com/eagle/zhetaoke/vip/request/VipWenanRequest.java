package com.eagle.zhetaoke.vip.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class VipWenanRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String page;
    private String pageSize;
    private String sort;
    private String totalCount;
}
