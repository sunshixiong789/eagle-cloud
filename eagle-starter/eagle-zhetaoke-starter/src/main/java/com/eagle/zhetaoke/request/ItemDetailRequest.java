package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品详情查询请求参数。
 *
 * @author 孙士雄
 */
@Data
public class ItemDetailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 淘客账号授权 ID sid。 */
    private String sid;

    /** 淘客 PID。 */
    private String pid;

    /** 商品 ID（必填）。 */
    private String taoId;

    /** 折淘客编号（可选）。 */
    private String code;

    /** 多个商品 ID 串，逗号分隔（可选）。 */
    private String numIids;

    /** 是否返回全部数据：0=全部，1=S券单条。 */
    private String type;
}
