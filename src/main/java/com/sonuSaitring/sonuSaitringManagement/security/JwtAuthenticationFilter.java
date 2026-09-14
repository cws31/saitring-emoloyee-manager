package com.sonuSaitring.sonuSaitringManagement.security;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

        private final JwtService jwtService;

        private final Counter tokenPresentCounter;
        private final Counter authenticationSuccessCounter;
        private final Counter authenticationFailureCounter;

        public JwtAuthenticationFilter(
                        JwtService jwtService,
                        MeterRegistry meterRegistry) {

                this.jwtService = jwtService;

                this.tokenPresentCounter = Counter.builder(
                                "security.jwt.token.present")
                                .description("Requests containing a Bearer token")
                                .register(meterRegistry);

                this.authenticationSuccessCounter = Counter.builder(
                                "security.jwt.authentication.success")
                                .description("Successful JWT authentications")
                                .register(meterRegistry);

                this.authenticationFailureCounter = Counter.builder(
                                "security.jwt.authentication.failure")
                                .description("Failed JWT authentications")
                                .register(meterRegistry);
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                String authorizationHeader = request.getHeader("Authorization");

                /*
                 * No JWT supplied.
                 *
                 * The SecurityFilterChain will decide whether
                 * the endpoint requires authentication.
                 */
                if (authorizationHeader == null
                                || !authorizationHeader.startsWith("Bearer ")) {

                        filterChain.doFilter(request, response);
                        return;
                }

                tokenPresentCounter.increment();

                String token = authorizationHeader.substring(7).trim();

                /*
                 * Ignore an empty Bearer token.
                 */
                if (token.isEmpty()) {

                        authenticationFailureCounter.increment();

                        log.debug(
                                        "Empty Bearer token received method={} uri={}",
                                        request.getMethod(),
                                        request.getRequestURI());

                        filterChain.doFilter(request, response);
                        return;
                }

                try {

                        /*
                         * Don't overwrite an authentication that may already
                         * have been established by another mechanism.
                         */
                        if (SecurityContextHolder.getContext()
                                        .getAuthentication() == null) {

                                if (jwtService.isTokenValid(token)) {

                                        String ownerId = jwtService.extractOwnerId(token);

                                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                        ownerId,
                                                        null,
                                                        Collections.emptyList());

                                        SecurityContextHolder.getContext()
                                                        .setAuthentication(authentication);

                                        authenticationSuccessCounter.increment();

                                        log.debug(
                                                        "JWT authentication successful ownerId={} method={} uri={}",
                                                        ownerId,
                                                        request.getMethod(),
                                                        request.getRequestURI());

                                } else {

                                        authenticationFailureCounter.increment();

                                        log.debug(
                                                        "Invalid JWT token method={} uri={}",
                                                        request.getMethod(),
                                                        request.getRequestURI());
                                }
                        }

                } catch (Exception ex) {

                        authenticationFailureCounter.increment();

                        SecurityContextHolder.clearContext();

                        /*
                         * Never log the JWT itself.
                         */
                        log.debug(
                                        "JWT authentication failed method={} uri={} errorType={}",
                                        request.getMethod(),
                                        request.getRequestURI(),
                                        ex.getClass().getSimpleName());
                }

                /*
                 * Important:
                 * The filter doesn't decide whether the request is allowed.
                 * SecurityConfig does that through authenticated()/permitAll().
                 */
                filterChain.doFilter(request, response);
        }
}