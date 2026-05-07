package com.eagle.zhetaoke.pdd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class PddGoodsDetailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String pddAppKey;
    private String pddAppSecret;
    private String pid;
    private String content;
}
