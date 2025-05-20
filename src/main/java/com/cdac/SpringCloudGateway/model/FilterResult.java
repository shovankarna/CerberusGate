package com.cdac.SpringCloudGateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Result returned by any GatewaySecurityFilter to represent outcome.
 */
@Data
@Builder
@AllArgsConstructor
public class FilterResult {
    private boolean allowed;
    private HttpStatus status;
    private String message;

    public static FilterResult allow() {
        return new FilterResult(true, HttpStatus.OK, "Allowed");
    }

    public static FilterResult deny(HttpStatus status, String message) {
        return new FilterResult(false, status, message);
    }
}
