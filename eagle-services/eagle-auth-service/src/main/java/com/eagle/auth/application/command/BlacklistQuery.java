package com.eagle.auth.application.command;

import com.eagle.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单查询参数
 *
 * @author sunshixiong
 */
public record BlacklistQuery(BlacklistType type, String value) {
}
