package com.eagle.auth.core.domain.model.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 淘宝绑定信息（值对象）。
 *
 * <p>仿 {@link WechatBinding}：不可变、无标识、整体替换。
 * {@code openUid} 为淘宝开放平台稳定用户标识（TOP {@code taobao.top.auth.token.create} 解析得到）。
 *
 * @author sunshixiong
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TaobaoBinding {

    @Column(name = "taobao_open_uid", length = 64, comment = "淘宝开放平台 openUid")
    private String openUid;

    @Column(name = "taobao_bind_time", comment = "淘宝绑定时间")
    private LocalDateTime bindTime;

    public static TaobaoBinding create(String openUid) {
        return new TaobaoBinding(openUid, LocalDateTime.now());
    }
}
