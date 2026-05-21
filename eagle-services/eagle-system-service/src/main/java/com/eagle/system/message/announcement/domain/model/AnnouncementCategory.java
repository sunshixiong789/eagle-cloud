package com.eagle.system.message.announcement.domain.model;

/**
 * 公告分类。
 *
 * <p>独立于 {@link com.eagle.system.message.domain.model.MessageCategory}：
 * 公告本身就是消息分类的一种，再用 MessageCategory 嵌套语义混淆。
 *
 * @author sunshixiong
 */
public enum AnnouncementCategory {

    /** 系统通知：账号策略、安全公告等。 */
    SYSTEM,

    /** 停服维护：例行升级、紧急维护等。 */
    MAINTENANCE,

    /** 活动通知：营销活动、新功能上线。 */
    ACTIVITY,

    /** 政策更新：用户协议、隐私政策变更。 */
    POLICY,

    /** 其他。 */
    OTHER
}
