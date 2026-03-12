package com.eleganteer.system.system.domain.model.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * 地址
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/9-09:25
 */
@Value
@Embeddable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Address {

    @Column(comment = "详细地址")
    String detailAddress;

    @Column(length = 500, comment = "街道")
    String street;

    @Column(length = 100, comment = "市")
    String city;

    @Column(length = 100, comment = "省")
    String state;

    @Column(length = 100, comment = "国家")
    String country;

    @Column(length = 20, comment = "邮编")
    String zipCode;
}
