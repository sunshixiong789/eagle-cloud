package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建推广位请求参数。
 *
 * @author 孙士雄
 */
@Data
public class CreatePidRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 推广位名称。 */
    private String adzoneName;

    /** 站点 ID。 */
    private String siteId;

    /** 媒体类型：1=PC，2=无线。 */
    private String mediaType;
}
