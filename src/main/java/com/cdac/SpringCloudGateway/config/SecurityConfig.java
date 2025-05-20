package com.cdac.SpringCloudGateway.config;

import java.util.List;
import org.apache.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;

import com.cdac.SpringCloudGateway.auth.GenericAuthorizationService;
import com.cdac.SpringCloudGateway.filters.UnifiedSecurityFilter;
import com.cdac.SpringCloudGateway.jwt.TokenDetails;
import com.cdac.SpringCloudGateway.model.SecurityMetadata;
import com.cdac.SpringCloudGateway.util.AuthorizationUtil;
import com.cdac.SpringCloudGateway.util.ErrorResponseUtil;
import com.cdac.SpringCloudGateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Configures the gateway's security filters and authorization logic.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GenericAuthorizationService authorizationService;
    private final UnifiedSecurityFilter unifiedSecurityFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> Mono.just(authentication);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authenticationManager(reactiveAuthenticationManager()) // Add this line
                .httpBasic(httpBasic -> httpBasic
                        .authenticationEntryPoint((exchange, ex) -> {
                            // Don't commit any response - let your filter handle it
                            return Mono.empty();
                        }))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterAt(unifiedSecurityFilter, SecurityWebFiltersOrder.FIRST)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/public/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyExchange().access((authMono, context) -> authorize(authMono, context)))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            // Don't commit response if already handled by your filter
                            if (exchange.getResponse().isCommitted()) {
                                return Mono.empty();
                            }
                            // Otherwise handle normally
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    private Mono<AuthorizationDecision> authorize(Mono<org.springframework.security.core.Authentication> auth,
            AuthorizationContext context) {
        ServerWebExchange exchange = context.getExchange();
        SecurityMetadata metadata = exchange.getAttribute(SecurityMetadata.class.getName());

        if (metadata == null || !metadata.isSecured()) {
            return Mono.just(new AuthorizationDecision(true));
        }

        String token = JwtUtil.extractBearerToken(exchange);
        if (token == null) {
            return ErrorResponseUtil.respond(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid token")
                    .thenReturn(new AuthorizationDecision(false));
        }

        return authorizationService.verifyToken(token)
                .flatMap(valid -> {
                    if (!valid) {
                        return ErrorResponseUtil.respond(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT signature")
                                .thenReturn(new AuthorizationDecision(false));
                    }

                    try {
                        TokenDetails details = exchange.getAttribute(TokenDetails.class.getName());
                        if (details == null) {
                            details = authorizationService.extractDetails(token);
                            log.warn("TokenDetails not found in context. Extracting manually again.");
                            exchange.getAttributes().put(TokenDetails.class.getName(), details);
                        }
                        List<String> roles = details.getRoles();
                        log.info("✅ Extracted userId: {}", details.getUserId());
                        log.info("✅ Extracted roles: {}", details.getRoles());
                        log.info("🔐 Required roles from route metadata: {}", metadata.getRoles());

                        boolean allowed = AuthorizationUtil.hasAnyMatchingRole(roles, metadata.getRoles());

                        if (!allowed) {
                            return ErrorResponseUtil
                                    .respond(exchange, HttpStatus.FORBIDDEN, "Access denied: Insufficient roles")
                                    .thenReturn(new AuthorizationDecision(false));
                        }

                        return Mono.just(new AuthorizationDecision(true));

                    } catch (Exception ex) {
                        return ErrorResponseUtil.respond(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT structure")
                                .thenReturn(new AuthorizationDecision(false));
                    }
                });
    }
}
