package com.eagle.system.message.announcement.interfaces.exception;

import com.eagle.common.exception.ErrorCode;

/**
 * 公告模块错误码（30551-30599）。
 *
 * @author sunshixiong
 */
public enum AnnouncementErrorCode implements ErrorCode {

    ANNOUNCEMENT_NOT_FOUND(30551, "announcement.not_found", "公告不存在"),
    ANNOUNCEMENT_INVALID(30552, "announcement.invalid", "公告参数非法"),
    ANNOUNCEMENT_REVOKED(30553, "announcement.revoked", "公告已撤回");

    private final ErrorCode.Meta meta;

    AnnouncementErrorCode(int code, String messageKey, String defaultMessage) {
        this.meta = new ErrorCode.Meta(code, messageKey, defaultMessage);
    }

    @Override
    public ErrorCode.Meta meta() {
        return meta;
    }
}
