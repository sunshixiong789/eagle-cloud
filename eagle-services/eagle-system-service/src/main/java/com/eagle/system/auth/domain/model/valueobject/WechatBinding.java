package com.eagle.system.auth.domain.model.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 微信绑定信息（值对象）
 * <p>
 * 值对象特征：
 * <ul>
 *   <li>不可变：通过创建新对象来修改</li>
 *   <li>无标识：通过属性值判断相等性</li>
 *   <li>可替换：整体替换而非修改属性</li>
 * </ul>
 * <p>
 * 支持三种微信平台的 openid：
 * <ul>
 *   <li>{@code openid}：微信小程序 openid</li>
 *   <li>{@code webOpenid}：微信开放平台网站应用 openid（PC 扫码）</li>
 *   <li>{@code mpOpenid}：微信公众号网页授权 openid（H5）</li>
 *   <li>{@code unionid}：跨平台联合 ID，同一微信用户在同一开放平台账号下唯一</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WechatBinding {

    @Column(length = 128, comment = "微信小程序 openid")
    private String openid;

    @Column(length = 128, comment = "微信 unionid")
    private String unionid;

    @Column(length = 128, name = "web_openid", comment = "微信网页（PC 扫码）openid")
    private String webOpenid;

    @Column(length = 128, name = "mp_openid", comment = "微信公众号网页授权 openid")
    private String mpOpenid;

    @Column(comment = "绑定时间")
    private LocalDateTime bindTime;

    /**
     * 创建小程序微信绑定
     */
    public static WechatBinding create(String openid, String unionid) {
        return new WechatBinding(openid, unionid, null, null, LocalDateTime.now());
    }

    /**
     * 创建 PC 扫码（开放平台网站应用）微信绑定
     */
    public static WechatBinding createForWeb(String webOpenid, String unionid) {
        return new WechatBinding(null, unionid, webOpenid, null, LocalDateTime.now());
    }

    /**
     * 创建公众号 H5 微信绑定
     */
    public static WechatBinding createForH5(String mpOpenid, String unionid) {
        return new WechatBinding(null, unionid, null, mpOpenid, LocalDateTime.now());
    }

    /**
     * 设置 PC 扫码 openid，返回新对象（不可变）
     */
    public WechatBinding withWebOpenid(String webOpenid) {
        return new WechatBinding(this.openid, this.unionid, webOpenid, this.mpOpenid, this.bindTime);
    }

    /**
     * 设置公众号 H5 openid，返回新对象（不可变）
     */
    public WechatBinding withMpOpenid(String mpOpenid) {
        return new WechatBinding(this.openid, this.unionid, this.webOpenid, mpOpenid, this.bindTime);
    }

    /**
     * 设置 unionid，返回新对象（不可变）
     */
    public WechatBinding withUnionid(String unionid) {
        return new WechatBinding(this.openid, unionid, this.webOpenid, this.mpOpenid, this.bindTime);
    }
}
