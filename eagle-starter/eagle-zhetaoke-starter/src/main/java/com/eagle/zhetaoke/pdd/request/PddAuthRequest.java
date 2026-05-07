package com.eagle.zhetaoke.pdd.request;

import lombok.Data;
import java.io.Serializable;

@Data
public class PddAuthRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String appkey;
    private String pddAppKey;
    private String pddAppSecret;
    private String pid;
    private String customParameters;
    private String generateQqApp;
    private String generateWeApp;
}
