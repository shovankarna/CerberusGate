package com.cdac.SpringCloudGateway.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class UnifiedSecurityFilter implements WebFilter {

    private final RouteLocator routeLocator;
    private final GatewayRateLimitFilter rateLimitFilter;
    private final GatewayNonceFilter nonceFilter;

    public UnifiedSecurityFilter(RouteLocator routeLocator,
            GatewayRateLimitFilter rateLimitFilter,
            GatewayNonceFilter nonceFilter) {
        this.routeLocator = routeLocator;
        this.rateLimitFilter = rateLimitFilter;
        this.nonceFilter = nonceFilter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Fetch routes directly from RouteLocator
        return routeLocator.getRoutes()
                .filterWhen(route -> route.getPredicate().apply(exchange)) // Match predicates reactively
                .next()
                .flatMap(matchingRoute -> {
                    if (matchingRoute != null) {
                        System.out.println("Route matched: " + matchingRoute.getId());

                        Map<String, Object> securityConfig = (Map<String, Object>) matchingRoute.getMetadata()
                                .get("securityConfig");

                        if (securityConfig != null) {
                            applySecurityMetadata(exchange, securityConfig);
                        }

                        // Step: Rate Limiting
                        if (!rateLimitFilter.applyRateLimit(exchange)) {
                            System.out.println("RATE LIMIT 429");
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                        System.out.println("RATE LIMIT ALLOWED");

                        // Step: Nonce Validation
                        if (!nonceFilter.validateNonce(exchange)) {
                            System.out.println("NONCE 401");
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }
                        System.out.println("Nonce ALLOWED");

                        // Proceed to the next filter if all checks pass
                        return chain.filter(exchange);
                    } else {
                        System.out.println("No route matched for request: " + exchange.getRequest().getURI());
                    }
                    return chain.filter(exchange); // If no matching route, proceed
                });
    }

    private void applySecurityMetadata(ServerWebExchange exchange, Map<String, Object> config) {
        boolean isSecured = (boolean) config.getOrDefault("isSecured", true);
        exchange.getAttributes().put("isSecured", isSecured);

        // Handle roles similar to how SecurityMetadataFilter did it
        if (config.containsKey("roles")) {
            Object rolesObj = config.get("roles");
            if (rolesObj instanceof List) {
                // If roles is a List, add directly
                exchange.getAttributes().put("roles", rolesObj);
            } else if (rolesObj instanceof Map) {
                // If roles is a Map, extract values and convert to List
                List<String> rolesList = ((Map<String, String>) rolesObj).values().stream()
                        .collect(Collectors.toList());
                exchange.getAttributes().put("roles", rolesList);
            }
        }

        // Handle rate-limiting configuration
        if (config.containsKey("rateLimit")) {
            exchange.getAttributes().put("rateLimit", config.get("rateLimit"));
        }

        // Handle nonce enabling configuration
        if (config.containsKey("nonceEnabled")) {
            boolean nonceEnabled = (boolean) config.get("nonceEnabled");
            exchange.getAttributes().put("nonceEnabled", nonceEnabled);
        }
    }
}
