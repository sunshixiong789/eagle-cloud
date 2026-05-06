package com.eagle.zhetaoke.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 转链请求参数。
 *
 * <p>覆盖京东、美团、饿了么、唯品会、考拉、拼多多、抖音等平台的转链。
 *
 * @author 孙士雄
 */
@Data
public class LinkConvertRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客对接秘钥 appkey。 */
    private String appkey;

    /** 原始链接或推广物料 URL。 */
    private String link;

    /** 推广物料 URL（京东专用）。 */
    private String materialId;

    /** 联盟 ID（京东 unionId / 推广位 pid）。 */
    private String unionId;

    /** 自定义推广位 ID。 */
    private String positionId;

    /** 转链类型：1=长链，2=短链，3=长链+短链，默认 2。 */
    private String chainType = "2";

    /** 平台类型。 */
    private String platform;

    /** 推广位 PID。 */
    private String pid;

    /** 优惠券链接（京东专用）。 */
    private String couponUrl;

    /** 子渠道标识（京东专用）。 */
    private String subUnionId;

    /** 微信小程序类型：1=京小街，2=京东购物。 */
    private String weChatType;

    /** 礼金批次号（京东专用）。 */
    private String giftCouponKey;

    /** 返回类型：0=官方结果，5=整合详情。 */
    private String signurl;
}
