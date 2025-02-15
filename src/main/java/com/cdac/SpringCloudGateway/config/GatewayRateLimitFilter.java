package com.cdac.SpringCloudGateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

@Component
public class GatewayRateLimitFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean applyRateLimit(ServerWebExchange exchange) {
        try {
            Map<String, Object> rateLimitConfig = exchange.getAttribute("rateLimit");

            if (rateLimitConfig != null && (boolean) rateLimitConfig.get("enabled")) {
                String key = createKey(exchange);
                Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(rateLimitConfig));

                // Return true if consumption is successful (i.e., allowed)
                return bucket.tryConsume(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // Default to true if rate limiting is not enabled or an error occurs
        return true;
    }

    private Bucket createNewBucket(Map<String, Object> config) {
        long capacity = (int) config.get("capacity");
        long refillTokens = (int) config.get("refillTokens");
        long refillPeriod = (int) config.get("refillPeriod");
        String refillUnit = (String) config.get("refillUnit");

        Duration duration = Duration.of(refillPeriod, java.time.temporal.ChronoUnit.valueOf(refillUnit));
        Refill refill = Refill.greedy(refillTokens, duration);
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return new LocalBucketBuilder().addLimit(limit).build();
    }

    private String createKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String remoteAddress = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        // Try to get the user ID from the JWT token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        String userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            TokenDetails td = TokenExtract.getTokenDetails(token.substring(7));
            userId = td.getUserId();
        }

        // Use user ID if available, otherwise use IP address
        String key = (userId != null) ? userId : remoteAddress;
        return key + ":" + path;
    }
}