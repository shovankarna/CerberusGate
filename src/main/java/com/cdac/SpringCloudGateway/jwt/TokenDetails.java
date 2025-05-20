package com.cdac.SpringCloudGateway.jwt;

import lombok.Data;

import java.util.List;

/**
 * Encapsulates details extracted from JWT for authorization.
 */
@Data
public class TokenDetails {
    private String userId;
    private List<String> roles;
}