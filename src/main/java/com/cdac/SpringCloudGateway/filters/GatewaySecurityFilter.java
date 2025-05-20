package com.cdac.SpringCloudGateway.filters;

import org.springframework.web.server.ServerWebExchange;

import com.cdac.SpringCloudGateway.model.FilterResult;
import com.cdac.SpringCloudGateway.model.SecurityMetadata;

/**
 * Interface for pluggable security filters.
 * Implementations can check request attributes, headers, metadata, etc.
 */
public interface GatewaySecurityFilter {
    boolean supports(SecurityMetadata metadata);

    FilterResult apply(ServerWebExchange exchange, SecurityMetadata metadata);

    default int getOrder() {
        return 0;
    } // Optional: order filters if needed
}
