package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 折淘客转链结果。
 *
 * <p>覆盖京东、美团、饿了么、唯品会、考拉、拼多多、抖音等平台的转链响应。
 *
 * @author 孙士雄
 */
@Data
public class ZhetaokeLinkResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 转链状态：200 表示成功。 */
    private Integer status;

    /** 转链后的短链接。 */
    private String shortUrl;

    /** 转链后的长链接。 */
    private String longUrl;

    /** 微信小程序短链。 */
    private String wxShortLink;

    /** 淘口令（淘宝相关）。 */
    private String tkl;

    /** 原始链接。 */
    private String originalUrl;

    /** 佣金比例。 */
    private String commissionRate;

    /** 佣金金额。 */
    private String commissionAmount;

    /** 商品标题。 */
    private String title;

    /** 商品图片。 */
    private String pictUrl;

    /** 券后价。 */
    private String quanhouJiage;

    /** 优惠券信息。 */
    private String couponInfo;

    /** 错误信息。 */
    private String msg;

    /**
     * 判断是否转链成功。
     *
     * @return true 当 status == 200
     */
    public boolean isSuccess() {
        return status != null && status == 200;
    }
}
