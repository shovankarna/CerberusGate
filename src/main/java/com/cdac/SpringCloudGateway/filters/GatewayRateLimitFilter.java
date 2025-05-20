package com.cdac.SpringCloudGateway.filters;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.cdac.SpringCloudGateway.jwt.TokenContextHolder;
import com.cdac.SpringCloudGateway.jwt.TokenDetails;
import com.cdac.SpringCloudGateway.model.FilterResult;
import com.cdac.SpringCloudGateway.model.SecurityMetadata;
import com.cdac.SpringCloudGateway.util.ErrorResponseUtil;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements in-memory rate limiting logic using Bucket4j.
 * Can later be replaced with Redis backend.
 */
@Slf4j
@Component
public class GatewayRateLimitFilter implements GatewaySecurityFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean supports(SecurityMetadata metadata) {
        return metadata.getRateLimit() != null && metadata.getRateLimit().isEnabled();
    }

    @Override
    public FilterResult apply(ServerWebExchange exchange, SecurityMetadata metadata) {
        SecurityMetadata.RateLimitConfig config = metadata.getRateLimit();
        String key = generateKey(exchange, config.getKeyStrategy());

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(config));
        boolean allowed = bucket.tryConsume(1);

        if (allowed) {
            return FilterResult.allow();
        } else {
            ErrorResponseUtil.respond(exchange, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded").subscribe();
            return FilterResult.deny(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private Bucket createBucket(SecurityMetadata.RateLimitConfig config) {
        var duration = java.time.Duration.of(config.getRefillPeriod(), ChronoUnit.valueOf(config.getRefillUnit()));
        var refill = Refill.greedy(config.getRefillTokens(), duration);
        var limit = Bandwidth.classic(config.getCapacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String generateKey(ServerWebExchange exchange, String strategy) {
        String path = exchange.getRequest().getPath().value();
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        TokenDetails details = TokenContextHolder.get(exchange);
        String userId = (details != null) ? details.getUserId() : null;

        return switch (strategy != null ? strategy.toLowerCase() : "") {
            case "user" -> userId != null ? userId : ip;
            case "path" -> path;
            case "ip" -> ip;
            case "user+path" -> userId != null ? userId + ":" + path : ip + ":" + path;
            case "ip+path" -> ip + ":" + path;
            default -> ip + ":" + path;
        };
    }
}