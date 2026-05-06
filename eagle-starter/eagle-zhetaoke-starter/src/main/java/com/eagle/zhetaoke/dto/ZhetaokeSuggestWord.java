package com.eagle.zhetaoke.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 联想词信息。
 *
 * @author 孙士雄
 */
@Data
public class ZhetaokeSuggestWord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 联想词。 */
    private String word;

    /** 搜索结果数量。 */
    private String resultCount;
}
