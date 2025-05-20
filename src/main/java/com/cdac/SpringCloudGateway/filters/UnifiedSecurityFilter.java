package com.cdac.SpringCloudGateway.filters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.cdac.SpringCloudGateway.auth.GenericAuthorizationService;
import com.cdac.SpringCloudGateway.jwt.TokenContextHolder;
import com.cdac.SpringCloudGateway.jwt.TokenDetails;
import com.cdac.SpringCloudGateway.model.FilterResult;
import com.cdac.SpringCloudGateway.model.SecurityMetadata;
import com.cdac.SpringCloudGateway.routes.RouteCache;
import com.cdac.SpringCloudGateway.util.JwtUtil;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unified filter that applies route-level security filters dynamically.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedSecurityFilter implements WebFilter {

    private final RouteCache routeCache;
    private final List<GatewaySecurityFilter> gatewayFilters;
    private final GenericAuthorizationService authorizationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return routeCache.getMatchingRoute(exchange)
                .doOnNext(route -> log.info("Route metadata: {}", route.getMetadata()))
                .flatMap(route -> {
                    log.debug("Matched route: {}", route.getId());

                    SecurityMetadata metadata = parseSecurityMetadata(route);
                    exchange.getAttributes().put(SecurityMetadata.class.getName(), metadata);

                    // Extract and store JWT context
                    String token = JwtUtil.extractBearerToken(exchange);
                    if (token != null) {
                        try {
                            TokenDetails details = authorizationService.extractDetails(token);
                            TokenContextHolder.set(exchange, details);
                        } catch (Exception ex) {
                            log.warn("Failed to extract token details: {}", ex.getMessage());
                        }
                    }

                    // Apply filters dynamically
                    for (GatewaySecurityFilter filter : gatewayFilters) {
                        if (filter.supports(metadata)) {
                            FilterResult result = filter.apply(exchange, metadata);
                            if (!result.isAllowed()) {
                                return respond(exchange, result);
                            }
                        }
                    }

                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @SuppressWarnings("unchecked")
    private SecurityMetadata parseSecurityMetadata(Route route) {
        Map<String, Object> metadataMap = route.getMetadata();

        // Extract securityConfig map safely
        Map<String, Object> configMap = null;
        Object configObj = metadataMap.get("securityConfig");
        if (configObj instanceof Map<?, ?> map) {
            configMap = (Map<String, Object>) map;
        } else {
            configMap = Collections.emptyMap();
        }

        boolean isSecured = safeCast(configMap.get("isSecured"), true);

        // Safely cast roles
        List<String> roles = List.of();
        Object rolesObj = configMap.get("roles");

        if (rolesObj instanceof List<?> rawList) {
            roles = rawList.stream()
                    .filter(item -> item != null)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else if (rolesObj instanceof Map<?, ?> map) {
            // Fall back: some YAML parsers convert list to map like {0=ADMIN}
            log.warn("⚠️ 'roles' config parsed as Map instead of List — normalizing values");
            roles = map.values().stream()
                    .filter(item -> item != null)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else if (rolesObj instanceof String str) {
            // Accept single string role as a list of one
            roles = List.of(str);
        } else if (rolesObj != null) {
            log.warn("⚠️ Unexpected 'roles' config type: {} → roles will be empty",
                    rolesObj.getClass().getSimpleName());
        }

        // Safely parse rate limit config
        SecurityMetadata.RateLimitConfig rateLimitConfig = null;
        Object rlObj = configMap.get("rateLimit");
        if (rlObj instanceof Map<?, ?> rateLimitMap) {
            rateLimitConfig = SecurityMetadata.RateLimitConfig.builder()
                    .enabled(safeCast(rateLimitMap.get("enabled"), false))
                    .capacity(safeCast(rateLimitMap.get("capacity"), 5))
                    .refillTokens(safeCast(rateLimitMap.get("refillTokens"), 5))
                    .refillPeriod(safeCast(rateLimitMap.get("refillPeriod"), 60))
                    .refillUnit(safeCast(rateLimitMap.get("refillUnit"), "SECONDS"))
                    .keyStrategy(safeCast(rateLimitMap.get("keyStrategy"), "ip+path"))
                    .build();
        }

        return SecurityMetadata.builder()
                .isSecured(isSecured)
                .roles(roles)
                .rateLimit(rateLimitConfig)
                .customProvider(configMap.get("customProvider") != null
                        ? configMap.get("customProvider").toString()
                        : null)
                .build();
    }

    private Mono<Void> respond(ServerWebExchange exchange, FilterResult result) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty(); // Don't write again if already committed
        }

        exchange.getResponse().setStatusCode(result.getStatus());
        byte[] bytes = result.getMessage().getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().add("Content-Type", "text/plain");
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    /**
     * Safe cast with fallback.
     */
    @SuppressWarnings("unchecked")
    private <T> T safeCast(Object value, T defaultValue) {
        try {
            return (T) value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
