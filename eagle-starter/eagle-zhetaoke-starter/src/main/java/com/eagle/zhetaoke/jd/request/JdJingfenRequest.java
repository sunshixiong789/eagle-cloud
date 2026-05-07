package com.eagle.zhetaoke.jd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class JdJingfenRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String eliteId;
    private String pageIndex;
    private String pageSize;
    private String sortName;
    private String sort;
    private String pid;
    private String fields;
    private String forbidTypes;
}
