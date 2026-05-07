package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 淘宝联盟官方活动信息。
 *
 * @author 孙士雄
 */
@Data
public class TbActivity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动 ID。 */
    private String activityId;

    /** 活动名称。 */
    private String activityName;

    /** 活动开始时间。 */
    private String startTime;

    /** 活动结束时间。 */
    private String endTime;

    /** 活动链接。 */
    private String activityUrl;

    /** 活动类型。 */
    private String activityType;
}
