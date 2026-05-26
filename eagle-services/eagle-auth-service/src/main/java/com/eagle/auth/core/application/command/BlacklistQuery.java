package com.eagle.auth.core.application.command;

import com.eagle.auth.core.domain.model.enums.BlacklistType;

/**
 * 黑名单查询参数
 *
 * @author sunshixiong
 */
public record BlacklistQuery(BlacklistType type, String value) {
}
