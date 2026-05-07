package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 淘口令创建结果。
 *
 * @author 孙士雄
 */
@Data
public class TklResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 生成的淘口令，如：￥D4A8bKYVD4h￥。 */
    private String model;
}
