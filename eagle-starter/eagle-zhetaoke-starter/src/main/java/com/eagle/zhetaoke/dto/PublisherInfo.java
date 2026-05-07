package com.eagle.zhetaoke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 渠道备案信息。
 *
 * @author 孙士雄
 */
@Data
public class PublisherInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 渠道关系 ID。 */
    @JsonProperty("relation_id")
    private String relationId;

    /** 会员运营 ID。 */
    @JsonProperty("special_id")
    private String specialId;

    /** 账户名称。 */
    @JsonProperty("account_name")
    private String accountName;

    /** 账户类型。 */
    @JsonProperty("account_type")
    private String accountType;

    /** 邀请码。 */
    @JsonProperty("invite_code")
    private String inviteCode;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    @JsonProperty("create_time")
    private String createTime;
}
