package com.eagle.idempotency.support;

/**
 * Resolves idempotency tokens from the current HTTP request context.
 *
 * @author 孙士雄
 */
@FunctionalInterface
public interface IdempotencyTokenResolver {

    /**
     * Resolve a token by header name.
     *
     * @param headerName idempotency token header
     * @return token value, or null when absent
     */
    String resolveToken(String headerName);
}
