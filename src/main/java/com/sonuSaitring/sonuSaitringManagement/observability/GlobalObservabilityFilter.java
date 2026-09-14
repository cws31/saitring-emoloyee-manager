package com.sonuSaitring.sonuSaitringManagement.observability;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalObservabilityFilter
        extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            GlobalObservabilityFilter.class);

    private static final String TRACE_ID = "traceId";

    private static final String REQUEST_ID = "requestId";

    private static final String START_TIME = "observabilityStartTime";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String requestId = UUID.randomUUID()
                .toString();

        String traceId = getTraceId();

        MDC.put(
                REQUEST_ID,
                requestId);

        MDC.put(
                TRACE_ID,
                traceId);

        request.setAttribute(
                START_TIME,
                startTime);

        String method = request.getMethod();

        String uri = request.getRequestURI();

        String query = request.getQueryString();

        long contentLength = request.getContentLengthLong();

        log.info(
                "http_request_started method={} uri={} query={} requestId={} traceId={} contentLength={}",
                method,
                uri,
                sanitizeQuery(query),
                requestId,
                traceId,
                contentLength);

        try {

            filterChain.doFilter(
                    request,
                    response);

        } catch (Exception ex) {

            long duration = System.currentTimeMillis()
                    - startTime;

            log.error(
                    "http_request_failed method={} uri={} status={} durationMs={} requestId={} traceId={} exception={}",
                    method,
                    uri,
                    response.getStatus(),
                    duration,
                    requestId,
                    traceId,
                    ex.getClass().getSimpleName());

            throw ex;

        } finally {

            long duration = System.currentTimeMillis()
                    - startTime;

            int status = response.getStatus();

            if (status >= 500) {

                log.error(
                        "http_request_completed method={} uri={} status={} durationMs={} requestId={} traceId={}",
                        method,
                        uri,
                        status,
                        duration,
                        requestId,
                        traceId);

            } else if (status >= 400) {

                log.warn(
                        "http_request_completed method={} uri={} status={} durationMs={} requestId={} traceId={}",
                        method,
                        uri,
                        status,
                        duration,
                        requestId,
                        traceId);

            } else {

                log.info(
                        "http_request_completed method={} uri={} status={} durationMs={} requestId={} traceId={}",
                        method,
                        uri,
                        status,
                        duration,
                        requestId,
                        traceId);
            }

            MDC.remove(
                    REQUEST_ID);

            MDC.remove(
                    TRACE_ID);
        }
    }

    private String getTraceId() {

        String traceId = MDC.get(TRACE_ID);

        if (traceId != null &&
                !traceId.isBlank()) {

            return traceId;
        }

        return UUID.randomUUID()
                .toString();
    }

    private String sanitizeQuery(
            String query) {

        if (query == null ||
                query.isBlank()) {

            return "";
        }
        return "[present]";
    }
}