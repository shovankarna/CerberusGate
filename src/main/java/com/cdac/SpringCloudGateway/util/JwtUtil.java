package com.cdac.SpringCloudGateway.util;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class JwtUtil {
    public static String extractBearerToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
