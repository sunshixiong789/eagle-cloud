package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 高佣转链请求参数（淘宝）。
 *
 * <p>覆盖单商品高佣转链、批量高佣转链、淘口令高佣转链。
 *
 * @author 孙士雄
 */
@Data
public class HighCommissionConvertRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 淘客 PID。 */
    private String pid;

    /** 商品 ID。 */
    private String numIid;

    /** 多个商品 ID 列表（批量转链）。 */
    private List<String> numIids;

    /** 淘口令（淘口令转链）。 */
    private String tkl;

    /** 渠道关系 ID。 */
    private String relationId;

    /** 会员运营 ID。 */
    private String specialId;

    /** 外部 ID。 */
    private String externalId;

    /** 返回值类型：0/1/2=官方结果，3=整合解析+转链，4=整合简版详情+淘口令，5=整合详情+淘口令。 */
    private Integer signurl = 5;
}
