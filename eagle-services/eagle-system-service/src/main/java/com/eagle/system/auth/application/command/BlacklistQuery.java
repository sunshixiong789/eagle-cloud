package com.eagle.system.auth.application.command;

import com.eagle.system.auth.domain.model.enums.BlacklistType;

/**
 * 黑名单查询参数
 *
 * @author sunshixiong
 */
public record BlacklistQuery(BlacklistType type, String value) {
}
