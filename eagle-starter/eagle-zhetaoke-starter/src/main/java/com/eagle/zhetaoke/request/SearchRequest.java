package com.eagle.zhetaoke.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全网搜索商品请求参数。
 *
 * <p>继承 {@link BaseQueryRequest}，增加搜索特有参数。
 *
 * @author 孙士雄
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchRequest extends BaseQueryRequest {

    private static final long serialVersionUID = 1L;

    /** 搜索关键词。 */
    private String q;

    /** 官方物料 ID。 */
    private String materialId;

    /** 是否有券：1。 */
    private String youquan;

    /** 是否海外：1。 */
    private String haiwai;

    /** 是否好评：1。 */
    private String haoping;
}
