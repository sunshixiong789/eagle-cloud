package com.eagle.common.handler;

import com.eagle.common.dto.ErrorResult;
import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.ServiceException;
import com.eagle.common.observability.RequestIdWebFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Unified JSON exception handler for WebFlux applications.
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveGlobalExceptionHandler implements WebExceptionHandler, Ordered {

    private static final String ACCESS_DENIED_EXCEPTION =
            "org.springframework.security.access.AccessDeniedException";

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ErrorResult result = buildErrorResult(exchange, ex);
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(result.getStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = serialize(result);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ErrorResult buildErrorResult(ServerWebExchange exchange, Throwable ex) {
        Locale locale = exchange.getLocaleContext().getLocale();
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        ErrorResult result;

        if (ex instanceof NotFoundException notFoundException) {
            result = appError(HttpStatus.NOT_FOUND, notFoundException, path, locale);
            log.warn("Resource not found [{}]: {}", notFoundException.getErrorCode().getCode(),
                    result.getMessage(), ex);
        } else if (ex instanceof ConflictException conflictException) {
            result = appError(HttpStatus.CONFLICT, conflictException, path, locale);
            log.warn("Resource conflict [{}]: {}", conflictException.getErrorCode().getCode(),
                    result.getMessage(), ex);
        } else if (ex instanceof DomainException domainException) {
            result = appError(HttpStatus.BAD_REQUEST, domainException, path, locale);
            log.warn("Domain exception [{}]: {}", domainException.getErrorCode().getCode(),
                    result.getMessage(), ex);
        } else if (ex instanceof ServiceException serviceException) {
            result = appError(HttpStatus.INTERNAL_SERVER_ERROR, serviceException, path, locale);
            log.error("Service exception [{}]: {}", serviceException.getErrorCode().getCode(),
                    result.getMessage(), ex);
        } else if (ex instanceof WebExchangeBindException bindException) {
            String message = bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + resolveMessage(error.getDefaultMessage(), locale))
                    .collect(Collectors.joining("; "));
            log.warn("Validation failed: {}", message);
            result = ErrorResult.of(HttpStatus.BAD_REQUEST, message, path);
        } else if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            String message = resolveMessage(ex.getMessage(), locale);
            log.warn("Bad request: {}", message, ex);
            result = ErrorResult.of(HttpStatus.BAD_REQUEST, message, path);
        } else if (ACCESS_DENIED_EXCEPTION.equals(ex.getClass().getName())) {
            String message = resolveMessage("error.common.forbidden", locale);
            log.warn("Access denied: {} {}", exchange.getRequest().getMethod(), path);
            result = ErrorResult.of(HttpStatus.FORBIDDEN, message, path);
        } else {
            String message = resolveMessage("error.server.internal_error", locale);
            log.error("Unhandled WebFlux exception: {}", ex.getMessage(), ex);
            result = ErrorResult.of(HttpStatus.INTERNAL_SERVER_ERROR, message, path);
        }

        result.setRequestId(resolveRequestId(exchange));
        return result;
    }

    private ErrorResult appError(HttpStatus status, AppException exception, String path, Locale locale) {
        String message = exception.getLocalizedMessage(messageSource, locale);
        return ErrorResult.of(status, message, exception.getErrorCode().getCode(), path);
    }

    private String resolveMessage(String messageOrKey, Locale locale) {
        if (messageOrKey == null) {
            return resolveMessage("error.server.internal_error", locale);
        }
        return messageSource.getMessage(messageOrKey, null, messageOrKey, locale);
    }

    private String resolveRequestId(ServerWebExchange exchange) {
        Object requestId = exchange.getAttribute(RequestIdWebFilter.REQUEST_ID_ATTRIBUTE);
        return requestId instanceof String value ? value : null;
    }

    private byte[] serialize(ErrorResult result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JacksonException e) {
            return "{\"message\":\"Failed to serialize error response\"}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
