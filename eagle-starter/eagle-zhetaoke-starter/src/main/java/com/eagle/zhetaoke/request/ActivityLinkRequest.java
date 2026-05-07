package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 淘宝联盟官方活动转链请求参数。
 *
 * @author 孙士雄
 */
@Data
public class ActivityLinkRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 淘客 PID。 */
    private String pid;

    /** 官方活动 ID。 */
    private String activityId;

    /** 推广位 ID。 */
    private String adzoneId;

    /** 渠道关系 ID。 */
    private String relationId;

    /** 会员运营 ID。 */
    private String specialId;

    /** 会场 ID。 */
    private String unionId;

    /** 返回值类型。 */
    private Integer signurl;
}
