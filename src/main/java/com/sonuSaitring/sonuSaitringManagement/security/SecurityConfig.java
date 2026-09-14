package com.sonuSaitring.sonuSaitringManagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationFilter jwtAuthenticationFilter)
                        throws Exception {

                http

                                /*
                                 * CORS configuration is provided by CorsConfig.
                                 */
                                .cors(cors -> {
                                })

                                /*
                                 * This is a stateless REST API using JWT.
                                 */
                                .csrf(csrf -> csrf.disable())

                                /*
                                 * Never create an HTTP session for authentication.
                                 */
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                /*
                                 * JWT authentication happens before Spring's
                                 * UsernamePasswordAuthenticationFilter.
                                 */
                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class)

                                /*
                                 * Endpoint authorization.
                                 */
                                .authorizeHttpRequests(auth -> auth

                                                /*
                                                 * Owner authentication flow.
                                                 *
                                                 * These endpoints must be accessible
                                                 * without an existing JWT.
                                                 */
                                                .requestMatchers(
                                                                "/api/owners/register",
                                                                "/api/owners/login",
                                                                "/api/owners/verify-otp")
                                                .permitAll()

                                                /*
                                                 * Public uploaded files.
                                                 */
                                                .requestMatchers(
                                                                "/uploads/**")
                                                .permitAll()

                                                /*
                                                 * Application health endpoint.
                                                 */
                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/health/**")
                                                .permitAll()

                                                /*
                                                 * Swagger / OpenAPI.
                                                 */
                                                .requestMatchers(
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                /*
                                                 * Everything else requires a valid
                                                 * authenticated security context.
                                                 */
                                                .anyRequest().authenticated());

                return http.build();
        }
}