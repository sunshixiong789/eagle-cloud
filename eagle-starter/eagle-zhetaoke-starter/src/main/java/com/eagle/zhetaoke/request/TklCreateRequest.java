package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 淘口令创建请求参数。
 *
 * @author 孙士雄
 */
@Data
public class TklCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 口令弹框内容，长度大于 5 个字符。 */
    private String text;

    /** 口令跳转目标页 URL（需 URL encode）。 */
    private String url;

    /** 口令弹框 logo URL。 */
    private String logo;

    /** 返回值类型：0=直接返回结果。 */
    private Integer signurl = 0;

    /** 结果类型：0=只返回淘口令，1=返回官方原始文案。 */
    private String type;
}
