package com.eagle.zhetaoke.kaola.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class KaolaSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String keyWord;
    private String type;
    private String desc;
    private String pageNo;
    private String pageSize;
}
