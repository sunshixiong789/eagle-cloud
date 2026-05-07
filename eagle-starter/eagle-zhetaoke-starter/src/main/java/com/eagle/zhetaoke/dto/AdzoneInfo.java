package com.eagle.zhetaoke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 淘宝联盟推广位信息。
 *
 * @author 孙士雄
 */
@Data
public class AdzoneInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推广位 ID。 */
    @JsonProperty("adzone_id")
    private Long adzoneId;

    /** 推广位名称。 */
    @JsonProperty("adzone_name")
    private String adzoneName;

    /** 站点 ID。 */
    @JsonProperty("site_id")
    private Long siteId;

    /** 站点名称。 */
    @JsonProperty("site_name")
    private String siteName;
}
