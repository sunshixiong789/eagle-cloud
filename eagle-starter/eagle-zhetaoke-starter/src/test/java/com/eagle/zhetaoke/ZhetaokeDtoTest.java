package com.eagle.zhetaoke;

import com.eagle.zhetaoke.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 折淘客 DTO 序列化/反序列化测试。
 *
 * <p>覆盖所有 DTO 的序列化、反序列化、边界值和空值处理。
 *
 * @author 孙士雄
 */
class ZhetaokeDtoTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    // ==================== ZhetaokeResponse 测试 ====================

    @Test
    void shouldDeserializeItemListResponse() throws JsonProcessingException {
        String json = """
                {
                    "status": 200,
                    "content": [
                        {
                            "code": "6646",
                            "tao_id": "554832820990",
                            "title": "测试商品",
                            "quanhou_jiage": "16.90",
                            "tkrate3": "30.00"
                        }
                    ]
                }
                """;

        ZhetaokeResponse<List<ZhetaokeItem>> response = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(ZhetaokeResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, ZhetaokeItem.class)));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTaoId()).isEqualTo("554832820990");
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("测试商品");
    }

    @Test
    void shouldHandleErrorResponse() throws JsonProcessingException {
        String json = """
                {
                    "status": 500,
                    "msg": "系统错误"
                }
                """;

        ZhetaokeResponse<?> response = mapper.readValue(json, ZhetaokeResponse.class);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMsg()).isEqualTo("系统错误");
        assertThat(response.getContent()).isNull();
    }

    @Test
    void shouldHandleNullStatus() {
        ZhetaokeResponse<String> response = new ZhetaokeResponse<>();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isNull();
    }

    @Test
    void shouldHandleEmptyContent() throws JsonProcessingException {
        String json = """
                {
                    "status": 200,
                    "content": []
                }
                """;

        ZhetaokeResponse<List<?>> response = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(ZhetaokeResponse.class, List.class));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    void shouldSerializeResponse() throws JsonProcessingException {
        ZhetaokeResponse<String> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent("ok");

        String json = mapper.writeValueAsString(response);
        assertThat(json).contains("\"status\":200");
        assertThat(json).contains("\"content\":\"ok\"");
    }

    // ==================== ZhetaokeItem 完整字段测试 ====================

    @Test
    void shouldDeserializeItemWithAllFields() throws JsonProcessingException {
        String json = """
                {
                    "code": "6646",
                    "type_one_id": "1",
                    "tao_id": "554832820990",
                    "title": "商品短标题",
                    "jianjie": "商品简介",
                    "pict_url": "https://example.com/img.jpg",
                    "user_type": "1",
                    "seller_id": "123456",
                    "shop_dsr": "4.80",
                    "volume": "1000",
                    "size": "29.90",
                    "quanhou_jiage": "19.90",
                    "date_time_yongjin": "2024-01-01 12:00:00",
                    "tkrate3": "30.00",
                    "yongjin_type": "MKT",
                    "coupon_id": "abc123",
                    "coupon_start_time": "2024-01-01",
                    "coupon_end_time": "2024-01-31",
                    "coupon_info_money": "10",
                    "coupon_total_count": "10000",
                    "coupon_remain_count": "5000",
                    "coupon_info": "满80.00元减10元",
                    "juhuasuan": "1",
                    "taoqianggou": "0",
                    "haitao": "0",
                    "jiyoujia": "1",
                    "jinpaimaijia": "1",
                    "pinpai": "1",
                    "pinpai_name": "测试品牌",
                    "yunfeixian": "1",
                    "nick": "测试店铺",
                    "small_images": "https://a.jpg|https://b.jpg",
                    "white_image": "https://white.jpg",
                    "tao_title": "商品长标题",
                    "provcity": "浙江 杭州",
                    "shop_title": "店铺名",
                    "zhibo_url": "https://video.mp4",
                    "sellCount": "9999",
                    "commentCount": "100",
                    "favcount": "50",
                    "score1": "4.9",
                    "score2": "4.8",
                    "score3": "4.9",
                    "creditLevel": "15",
                    "shopIcon": "//img.logo.jpg",
                    "pcDescContent": "1.jpg|2.jpg",
                    "item_url": "https://item.taobao.com/item.htm?id=123",
                    "category_id": "50006126",
                    "category_name": "收纳盒",
                    "level_one_category_id": "122928002",
                    "level_one_category_name": "收纳整理",
                    "tkfee3": "5.07",
                    "biaoqian": "满300元省30元",
                    "tag": "朋友圈文案",
                    "date_time": "2024-01-01 10:00:00"
                }
                """;

        ZhetaokeItem item = mapper.readValue(json, ZhetaokeItem.class);

        assertThat(item.getCode()).isEqualTo("6646");
        assertThat(item.getTypeOneId()).isEqualTo("1");
        assertThat(item.getTaoId()).isEqualTo("554832820990");
        assertThat(item.getTitle()).isEqualTo("商品短标题");
        assertThat(item.getJianjie()).isEqualTo("商品简介");
        assertThat(item.getPictUrl()).isEqualTo("https://example.com/img.jpg");
        assertThat(item.getUserType()).isEqualTo("1");
        assertThat(item.getSellerId()).isEqualTo("123456");
        assertThat(item.getShopDsr()).isEqualTo("4.80");
        assertThat(item.getVolume()).isEqualTo("1000");
        assertThat(item.getSize()).isEqualTo("29.90");
        assertThat(item.getQuanhouJiage()).isEqualTo("19.90");
        assertThat(item.getDateTimeYongjin()).isEqualTo("2024-01-01 12:00:00");
        assertThat(item.getTkrate3()).isEqualTo("30.00");
        assertThat(item.getYongjinType()).isEqualTo("MKT");
        assertThat(item.getCouponId()).isEqualTo("abc123");
        assertThat(item.getCouponStartTime()).isEqualTo("2024-01-01");
        assertThat(item.getCouponEndTime()).isEqualTo("2024-01-31");
        assertThat(item.getCouponInfoMoney()).isEqualTo("10");
        assertThat(item.getCouponTotalCount()).isEqualTo("10000");
        assertThat(item.getCouponRemainCount()).isEqualTo("5000");
        assertThat(item.getCouponInfo()).isEqualTo("满80.00元减10元");
        assertThat(item.getJuhuasuan()).isEqualTo("1");
        assertThat(item.getTaoqianggou()).isEqualTo("0");
        assertThat(item.getHaitao()).isEqualTo("0");
        assertThat(item.getJiyoujia()).isEqualTo("1");
        assertThat(item.getJinpaiMaijia()).isEqualTo("1");
        assertThat(item.getPinpai()).isEqualTo("1");
        assertThat(item.getPinpaiName()).isEqualTo("测试品牌");
        assertThat(item.getYunfeixian()).isEqualTo("1");
        assertThat(item.getNick()).isEqualTo("测试店铺");
        assertThat(item.getSmallImages()).isEqualTo("https://a.jpg|https://b.jpg");
        assertThat(item.getWhiteImage()).isEqualTo("https://white.jpg");
        assertThat(item.getTaoTitle()).isEqualTo("商品长标题");
        assertThat(item.getProvcity()).isEqualTo("浙江 杭州");
        assertThat(item.getShopTitle()).isEqualTo("店铺名");
        assertThat(item.getZhiboUrl()).isEqualTo("https://video.mp4");
        assertThat(item.getSellCount()).isEqualTo("9999");
        assertThat(item.getCommentCount()).isEqualTo("100");
        assertThat(item.getFavcount()).isEqualTo("50");
        assertThat(item.getScore1()).isEqualTo("4.9");
        assertThat(item.getScore2()).isEqualTo("4.8");
        assertThat(item.getScore3()).isEqualTo("4.9");
        assertThat(item.getCreditLevel()).isEqualTo("15");
        assertThat(item.getShopIcon()).isEqualTo("//img.logo.jpg");
        assertThat(item.getPcDescContent()).isEqualTo("1.jpg|2.jpg");
        assertThat(item.getItemUrl()).isEqualTo("https://item.taobao.com/item.htm?id=123");
        assertThat(item.getCategoryId()).isEqualTo("50006126");
        assertThat(item.getCategoryName()).isEqualTo("收纳盒");
        assertThat(item.getLevelOneCategoryId()).isEqualTo("122928002");
        assertThat(item.getLevelOneCategoryName()).isEqualTo("收纳整理");
        assertThat(item.getTkfee3()).isEqualTo("5.07");
        assertThat(item.getBiaoqian()).isEqualTo("满300元省30元");
        assertThat(item.getTag()).isEqualTo("朋友圈文案");
        assertThat(item.getDateTime()).isEqualTo("2024-01-01 10:00:00");
    }

    @Test
    void shouldHandleItemWithNullFields() throws JsonProcessingException {
        String json = """
                {
                    "tao_id": "123",
                    "title": "仅含必填字段"
                }
                """;

        ZhetaokeItem item = mapper.readValue(json, ZhetaokeItem.class);
        assertThat(item.getTaoId()).isEqualTo("123");
        assertThat(item.getTitle()).isEqualTo("仅含必填字段");
        assertThat(item.getCode()).isNull();
        assertThat(item.getCouponId()).isNull();
        assertThat(item.getPinpaiName()).isNull();
    }

    // ==================== ZhetaokeLinkResult 测试 ====================

    @Test
    void shouldDeserializeLinkResult() throws JsonProcessingException {
        String json = """
                {
                    "status": 200,
                    "shortUrl": "https://u.jd.com/abc",
                    "longUrl": "https://union-click.jd.com/xxx",
                    "wxShortLink": "https://u.jd.com/wx",
                    "tkl": "￥ABC123￥",
                    "originalUrl": "https://item.jd.com/123.html",
                    "commissionRate": "30",
                    "commissionAmount": "5.00",
                    "title": "京东商品",
                    "pictUrl": "https://img.jd.com/1.jpg",
                    "quanhouJiage": "19.90",
                    "couponInfo": "满20减5"
                }
                """;

        ZhetaokeLinkResult result = mapper.readValue(json, ZhetaokeLinkResult.class);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getShortUrl()).isEqualTo("https://u.jd.com/abc");
        assertThat(result.getLongUrl()).isEqualTo("https://union-click.jd.com/xxx");
        assertThat(result.getWxShortLink()).isEqualTo("https://u.jd.com/wx");
        assertThat(result.getTkl()).isEqualTo("￥ABC123￥");
        assertThat(result.getOriginalUrl()).isEqualTo("https://item.jd.com/123.html");
        assertThat(result.getCommissionRate()).isEqualTo("30");
        assertThat(result.getCommissionAmount()).isEqualTo("5.00");
        assertThat(result.getTitle()).isEqualTo("京东商品");
        assertThat(result.getPictUrl()).isEqualTo("https://img.jd.com/1.jpg");
        assertThat(result.getQuanhouJiage()).isEqualTo("19.90");
        assertThat(result.getCouponInfo()).isEqualTo("满20减5");
    }

    @Test
    void shouldHandleLinkResultFailure() throws JsonProcessingException {
        String json = """
                {
                    "status": 400,
                    "msg": "转链失败"
                }
                """;

        ZhetaokeLinkResult result = mapper.readValue(json, ZhetaokeLinkResult.class);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMsg()).isEqualTo("转链失败");
    }

    @Test
    void shouldSerializeLinkResult() throws JsonProcessingException {
        ZhetaokeLinkResult result = new ZhetaokeLinkResult();
        result.setStatus(200);
        result.setShortUrl("https://short.url");

        String json = mapper.writeValueAsString(result);
        assertThat(json).contains("\"status\":200");
        assertThat(json).contains("\"shortUrl\":\"https://short.url\"");
    }

    // ==================== ZhetaokeOrder 测试 ====================

    @Test
    void shouldDeserializeOrder() throws JsonProcessingException {
        String json = """
                {
                    "orderId": "123456789",
                    "itemId": "987654321",
                    "itemTitle": "测试商品",
                    "itemImg": "https://img.jpg",
                    "orderAmount": "99.00",
                    "commissionAmount": "10.00",
                    "commissionRate": "10",
                    "orderStatus": "已结算",
                    "orderTime": "2024-01-01 10:00:00",
                    "settleTime": "2024-01-02 10:00:00",
                    "platform": "jd",
                    "positionId": "123",
                    "pid": "mm_1_2_3"
                }
                """;

        ZhetaokeOrder order = mapper.readValue(json, ZhetaokeOrder.class);

        assertThat(order.getOrderId()).isEqualTo("123456789");
        assertThat(order.getItemId()).isEqualTo("987654321");
        assertThat(order.getItemTitle()).isEqualTo("测试商品");
        assertThat(order.getItemImg()).isEqualTo("https://img.jpg");
        assertThat(order.getOrderAmount()).isEqualTo("99.00");
        assertThat(order.getCommissionAmount()).isEqualTo("10.00");
        assertThat(order.getCommissionRate()).isEqualTo("10");
        assertThat(order.getOrderStatus()).isEqualTo("已结算");
        assertThat(order.getOrderTime()).isEqualTo("2024-01-01 10:00:00");
        assertThat(order.getSettleTime()).isEqualTo("2024-01-02 10:00:00");
        assertThat(order.getPlatform()).isEqualTo("jd");
        assertThat(order.getPositionId()).isEqualTo("123");
        assertThat(order.getPid()).isEqualTo("mm_1_2_3");
    }

    @Test
    void shouldSerializeOrder() throws JsonProcessingException {
        ZhetaokeOrder order = new ZhetaokeOrder();
        order.setOrderId("123");
        order.setPlatform("mt");

        String json = mapper.writeValueAsString(order);
        assertThat(json).contains("\"orderId\":\"123\"");
        assertThat(json).contains("\"platform\":\"mt\"");
    }

    // ==================== ZhetaokeHotWord 测试 ====================

    @Test
    void shouldDeserializeHotWord() throws JsonProcessingException {
        String json = """
                {
                    "word": "手机",
                    "hotValue": "10000",
                    "rank": "1"
                }
                """;

        ZhetaokeHotWord hotWord = mapper.readValue(json, ZhetaokeHotWord.class);
        assertThat(hotWord.getWord()).isEqualTo("手机");
        assertThat(hotWord.getHotValue()).isEqualTo("10000");
        assertThat(hotWord.getRank()).isEqualTo("1");
    }

    @Test
    void shouldSerializeHotWord() throws JsonProcessingException {
        ZhetaokeHotWord hotWord = new ZhetaokeHotWord();
        hotWord.setWord("电脑");
        hotWord.setHotValue("5000");

        String json = mapper.writeValueAsString(hotWord);
        assertThat(json).contains("\"word\":\"电脑\"");
    }

    // ==================== ZhetaokeSuggestWord 测试 ====================

    @Test
    void shouldDeserializeSuggestWord() throws JsonProcessingException {
        String json = """
                {
                    "word": "苹果手机",
                    "resultCount": "100000"
                }
                """;

        ZhetaokeSuggestWord suggest = mapper.readValue(json, ZhetaokeSuggestWord.class);
        assertThat(suggest.getWord()).isEqualTo("苹果手机");
        assertThat(suggest.getResultCount()).isEqualTo("100000");
    }

    @Test
    void shouldSerializeSuggestWord() throws JsonProcessingException {
        ZhetaokeSuggestWord suggest = new ZhetaokeSuggestWord();
        suggest.setWord("华为");

        String json = mapper.writeValueAsString(suggest);
        assertThat(json).contains("\"word\":\"华为\"");
    }

    // ==================== 边界值测试 ====================

    @Test
    void shouldHandleStatusBoundaryValues() {
        ZhetaokeResponse<String> response = new ZhetaokeResponse<>();

        response.setStatus(200);
        assertThat(response.isSuccess()).isTrue();

        response.setStatus(199);
        assertThat(response.isSuccess()).isFalse();

        response.setStatus(201);
        assertThat(response.isSuccess()).isFalse();

        response.setStatus(null);
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void shouldHandleItemListResponse() throws JsonProcessingException {
        String json = """
                {
                    "status": 200,
                    "content": [
                        {"tao_id": "1", "title": "商品1"},
                        {"tao_id": "2", "title": "商品2"},
                        {"tao_id": "3", "title": "商品3"}
                    ]
                }
                """;

        ZhetaokeResponse<List<ZhetaokeItem>> response = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(ZhetaokeResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, ZhetaokeItem.class)));

        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getContent().get(2).getTitle()).isEqualTo("商品3");
    }

    @Test
    void shouldHandleFenciResponse() throws JsonProcessingException {
        String json = """
                {
                    "status": 200,
                    "content": ["手机", "壳", "保护", "套"]
                }
                """;

        ZhetaokeResponse<List<String>> response = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(ZhetaokeResponse.class,
                        mapper.getTypeFactory().constructCollectionType(List.class, String.class)));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getContent()).containsExactly("手机", "壳", "保护", "套");
    }
}
