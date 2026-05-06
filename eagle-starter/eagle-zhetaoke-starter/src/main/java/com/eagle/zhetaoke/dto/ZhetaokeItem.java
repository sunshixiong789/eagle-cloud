package com.eagle.zhetaoke.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 折淘客商品信息。
 *
 * <p>覆盖淘宝/天猫领券商品、全网搜索、商品详情等接口返回的商品数据结构。
 *
 * @author 孙士雄
 */
@Data
public class ZhetaokeItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折淘客编号。 */
    private String code;

    /** 一级分类 ID。 */
    @JsonProperty("type_one_id")
    private String typeOneId;

    /** 商品 ID（淘宝/天猫）。 */
    @JsonProperty("tao_id")
    private String taoId;

    /** 商品短标题。 */
    private String title;

    /** 商品简介。 */
    private String jianjie;

    /** 商品主图 URL。 */
    @JsonProperty("pict_url")
    private String pictUrl;

    /** 是否天猫：0=淘宝，1=天猫。 */
    @JsonProperty("user_type")
    private String userType;

    /** 卖家 ID。 */
    @JsonProperty("seller_id")
    private String sellerId;

    /** 商品描述分（动态评分）。 */
    @JsonProperty("shop_dsr")
    private String shopDsr;

    /** 年销量。 */
    private String volume;

    /** 折扣价。 */
    private String size;

    /** 券后价。 */
    @JsonProperty("quanhou_jiage")
    private String quanhouJiage;

    /** 数据更新时间。 */
    @JsonProperty("date_time_yongjin")
    private String dateTimeYongjin;

    /** 佣金比率。 */
    @JsonProperty("tkrate3")
    private String tkrate3;

    /** 佣金类型。 */
    @JsonProperty("yongjin_type")
    private String yongjinType;

    /** 优惠券 ID。 */
    @JsonProperty("coupon_id")
    private String couponId;

    /** 优惠券开始时间。 */
    @JsonProperty("coupon_start_time")
    private String couponStartTime;

    /** 优惠券结束时间。 */
    @JsonProperty("coupon_end_time")
    private String couponEndTime;

    /** 优惠券金额。 */
    @JsonProperty("coupon_info_money")
    private String couponInfoMoney;

    /** 优惠券总数量。 */
    @JsonProperty("coupon_total_count")
    private String couponTotalCount;

    /** 优惠券剩余数量。 */
    @JsonProperty("coupon_remain_count")
    private String couponRemainCount;

    /** 优惠券信息（如"满80.00元减10元"）。 */
    @JsonProperty("coupon_info")
    private String couponInfo;

    /** 是否聚划算：1=是。 */
    private String juhuasuan;

    /** 是否淘抢购：1=是。 */
    private String taoqianggou;

    /** 是否海淘：1=是。 */
    private String haitao;

    /** 是否极有家：1=是。 */
    private String jiyoujia;

    /** 是否金牌卖家：1=是。 */
    @JsonProperty("jinpaimaijia")
    private String jinpaiMaijia;

    /** 是否精选品牌：1=是。 */
    private String pinpai;

    /** 品牌名称。 */
    @JsonProperty("pinpai_name")
    private String pinpaiName;

    /** 是否有运费险：1=有。 */
    private String yunfeixian;

    /** 卖家昵称。 */
    private String nick;

    /** 商品小图列表（| 分隔）。 */
    @JsonProperty("small_images")
    private String smallImages;

    /** 商品白底图。 */
    @JsonProperty("white_image")
    private String whiteImage;

    /** 商品长标题。 */
    @JsonProperty("tao_title")
    private String taoTitle;

    /** 宝贝所在地。 */
    private String provcity;

    /** 店铺名称。 */
    @JsonProperty("shop_title")
    private String shopTitle;

    /** 视频地址。 */
    @JsonProperty("zhibo_url")
    private String zhiboUrl;

    /** 淘宝网页实时总销量。 */
    @JsonProperty("sellCount")
    private String sellCount;

    /** 评论数量。 */
    @JsonProperty("commentCount")
    private String commentCount;

    /** 收藏数量。 */
    @JsonProperty("favcount")
    private String favcount;

    /** 宝贝描述分。 */
    private String score1;

    /** 卖家服务分。 */
    private String score2;

    /** 物流服务分。 */
    private String score3;

    /** 店铺等级。 */
    @JsonProperty("creditLevel")
    private String creditLevel;

    /** 店铺 Logo。 */
    @JsonProperty("shopIcon")
    private String shopIcon;

    /** 图文详情图片地址（| 分隔）。 */
    @JsonProperty("pcDescContent")
    private String pcDescContent;

    /** 商品详情页 URL。 */
    @JsonProperty("item_url")
    private String itemUrl;

    /** 叶子类目 ID。 */
    @JsonProperty("category_id")
    private String categoryId;

    /** 叶子类目名称。 */
    @JsonProperty("category_name")
    private String categoryName;

    /** 一级类目 ID。 */
    @JsonProperty("level_one_category_id")
    private String levelOneCategoryId;

    /** 一级类目名称。 */
    @JsonProperty("level_one_category_name")
    private String levelOneCategoryName;

    /** 返佣金额。 */
    private String tkfee3;

    /** 店铺活动标签。 */
    private String biaoqian;

    /** 朋友圈文案（需 URL decode）。 */
    private String tag;

    /** 数据添加时间。 */
    @JsonProperty("date_time")
    private String dateTime;
}
