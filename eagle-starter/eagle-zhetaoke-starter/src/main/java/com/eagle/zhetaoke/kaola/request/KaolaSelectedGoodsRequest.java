package com.eagle.zhetaoke.kaola.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class KaolaSelectedGoodsRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String poolName;
    private String pageNo;
    private String pageSize;
}
