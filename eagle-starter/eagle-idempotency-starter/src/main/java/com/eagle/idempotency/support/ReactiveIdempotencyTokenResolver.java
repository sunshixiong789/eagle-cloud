package com.eagle.idempotency.support;

/**
 * WebFlux idempotency token resolver.
 *
 * @author eagle
 */
public class ReactiveIdempotencyTokenResolver implements IdempotencyTokenResolver {

    @Override
    public String resolveToken(String headerName) {
        return ReactiveIdempotencyTokenContext.get(headerName);
    }
}
