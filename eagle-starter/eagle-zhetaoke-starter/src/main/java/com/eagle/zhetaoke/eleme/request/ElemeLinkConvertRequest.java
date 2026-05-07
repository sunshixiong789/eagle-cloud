package com.eagle.zhetaoke.eleme.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class ElemeLinkConvertRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String sid;
    private String activityId;
    private String customerId;
}
