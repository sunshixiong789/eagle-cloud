package com.eagle.system.message.domain.model;

/**
 * 消息分类。
 *
 * <p>用于前端按分类分 tab 展示。新增分类时无需通知发布方——
 * 集成事件 {@code category} 字段为 String，发布方传入新值，消费方按规则映射；
 * 未识别值统一归入 {@link #SYSTEM}。
 *
 * @author sunshixiong
 */
public enum MessageCategory {

    /** 系统通知：账号、安全、平台公告等。 */
    SYSTEM,

    /** 交易类：返利到账、提现完成、订单状态等。 */
    TRADE,

    /** 营销类：活动、优惠券、推广通知等。 */
    MARKETING,

    /** 全员公告（区别于 SYSTEM 的针对性通知）。 */
    ANNOUNCEMENT;

    /**
     * 安全解析：未识别值降级为 {@link #SYSTEM}，避免发布方传错导致消息丢失。
     */
    public static MessageCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SYSTEM;
        }
        try {
            return MessageCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignore) {
            return SYSTEM;
        }
    }
}
