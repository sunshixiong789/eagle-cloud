package com.eagle.idempotency.support;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Servlet request based idempotency token resolver.
 *
 * @author eagle
 */
@RequiredArgsConstructor
public class ServletIdempotencyTokenResolver implements IdempotencyTokenResolver {

    private final HttpServletRequest request;

    @Override
    public String resolveToken(String headerName) {
        return request.getHeader(headerName);
    }
}
