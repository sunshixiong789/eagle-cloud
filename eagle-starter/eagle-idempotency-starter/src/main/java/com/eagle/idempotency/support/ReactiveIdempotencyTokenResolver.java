package com.eagle.idempotency.support;

/**
 * WebFlux idempotency token resolver.
 *
 * @author 孙士雄
 */
public class ReactiveIdempotencyTokenResolver implements IdempotencyTokenResolver {

    @Override
    public String resolveToken(String headerName) {
        return ReactiveIdempotencyTokenContext.get(headerName);
    }
}
