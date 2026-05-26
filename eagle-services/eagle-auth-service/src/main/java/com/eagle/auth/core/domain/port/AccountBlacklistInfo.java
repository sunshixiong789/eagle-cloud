package com.eagle.auth.core.domain.port;

/**
 * 账号黑名单信息。
 *
 * @param id    黑名单记录 ID
 * @param value 黑名单值
 */
public record AccountBlacklistInfo(Long id, String value) {
}
