package com.cdac.SpringCloudGateway.jwt;

import org.springframework.web.server.ServerWebExchange;

/**
 * Holds parsed token details in the request context.
 */
public class TokenContextHolder {
    private static final String ATTRIBUTE_KEY = TokenDetails.class.getName();

    public static void set(ServerWebExchange exchange, TokenDetails details) {
        exchange.getAttributes().put(ATTRIBUTE_KEY, details);
    }

    public static TokenDetails get(ServerWebExchange exchange) {
        return (TokenDetails) exchange.getAttribute(ATTRIBUTE_KEY);
    }
}
