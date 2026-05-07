package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 渠道备案请求参数。
 *
 * @author 孙士雄
 */
@Data
public class PublisherSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 渠道关系 ID（如已存在则更新）。 */
    private String relationId;

    /** 会员运营 ID。 */
    private String specialId;

    /** 账户名称。 */
    private String accountName;

    /** 账户类型。 */
    private String accountType;

    /** 邀请码。 */
    private String inviteCode;

    /** 备注。 */
    private String remark;

    /** 信息类型：1=渠道信息，2=会员信息。 */
    private String infoType;
}
