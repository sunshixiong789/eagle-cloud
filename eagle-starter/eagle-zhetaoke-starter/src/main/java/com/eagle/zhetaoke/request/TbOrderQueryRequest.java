package com.eagle.zhetaoke.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 淘宝订单查询请求参数。
 *
 * <p>覆盖淘宝联盟订单查询、维权订单查询。
 *
 * @author 孙士雄
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TbOrderQueryRequest extends BaseQueryRequest {

    private static final long serialVersionUID = 1L;

    /** 订单查询开始时间，格式：2019-04-05 12:18:22。 */
    private String startTime;

    /** 订单查询结束时间，格式：2019-04-25 15:18:22。 */
    private String endTime;

    /** 查询时间类型：1=创建时间，2=付款时间，3=结算时间，4=更新时间。 */
    private String queryType;

    /** 位点，翻页时传递。 */
    private String positionIndex;

    /** 推广者角色类型：2=二方，3=三方。 */
    private String memberType;

    /** 淘客订单状态：12=付款，13=关闭，14=确认收货，3=结算成功。 */
    private String tkStatus;

    /** 跳转类型：-1=向前翻页，1=向后翻页。 */
    private String jumpType;

    /** 第几页，默认 1。 */
    private String pageNo;

    /** 场景订单场景类型：1=常规订单，2=渠道订单，3=会员运营订单。 */
    private String orderScene;

    /** 返回值类型：1 或 2 表示直接返回订单结果信息。 */
    private Integer signurl;

    /** 维权类型：1=维权订单。 */
    private String refundType;

    /** 业务类型：1=淘宝。 */
    private String bizType;

    /** 是否简化返回：0=完整，1=简化。 */
    private String simplify;
}
