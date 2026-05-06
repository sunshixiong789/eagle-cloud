package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 热搜词信息。
 *
 * @author 孙士雄
 */
@Data
public class ZhetaokeHotWord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 热搜词。 */
    private String word;

    /** 搜索热度。 */
    private String hotValue;

    /** 排名。 */
    private String rank;
}
