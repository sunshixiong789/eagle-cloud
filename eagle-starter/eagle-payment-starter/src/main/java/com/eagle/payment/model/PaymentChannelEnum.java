package com.eagle.payment.model;

/**
 * 支付渠道枚举。
 *
 * <p>标识支付请求所使用的支付方式，业务方可通过此枚举路由到对应的
 * {@link com.eagle.payment.gateway.PaymentGateway} 实现。
 *
 * @author eagle
 */
public enum PaymentChannelEnum {

    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),

    /**
     * 微信支付
     */
    WECHAT("wechat", "微信支付"),

    /**
     * 余额支付
     */
    BALANCE("balance", "余额支付"),

    /**
     * 银行卡支付
     */
    BANK_CARD("bank_card", "银行卡");

    /**
     * 渠道编码（持久化、接口传输使用）
     */
    private final String code;

    /**
     * 渠道名称（展示使用）
     */
    private final String name;

    /**
     * 构造支付渠道枚举。
     *
     * @param code 渠道编码
     * @param name 渠道名称
     */
    PaymentChannelEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据渠道编码查找对应的枚举值。
     *
     * <p>大小写不敏感匹配，找不到时抛出 {@link IllegalArgumentException}。
     *
     * @param code 渠道编码
     * @return 对应枚举值
     * @throws IllegalArgumentException 当编码不存在时
     */
    public static PaymentChannelEnum fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Payment channel code must not be null");
        }
        for (PaymentChannelEnum channel : values()) {
            if (channel.code.equalsIgnoreCase(code)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown payment channel code: " + code);
    }

    /**
     * 获取渠道编码。
     *
     * @return 渠道编码字符串
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取渠道名称。
     *
     * @return 渠道名称字符串
     */
    public String getName() {
        return name;
    }
}
