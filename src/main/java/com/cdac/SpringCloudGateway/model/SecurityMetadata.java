package com.cdac.SpringCloudGateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Security metadata parsed from route definition.
 * Used for authorization and policy enforcement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityMetadata {
    private boolean isSecured;
    private List<String> roles;
    private RateLimitConfig rateLimit;
    private String customProvider;
    // Add more future fields like scopes, MFA, IP range etc.

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateLimitConfig {
        private boolean enabled;
        private int capacity;
        private int refillTokens;
        private int refillPeriod;
        private String refillUnit;
        private String keyStrategy; // NEW: e.g. "user", "ip+path", etc.
    }

}
