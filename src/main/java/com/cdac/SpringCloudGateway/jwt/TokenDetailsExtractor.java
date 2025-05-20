package com.cdac.SpringCloudGateway.jwt;

/**
 * Strategy interface for extracting TokenDetails from a JWT.
 */
public interface TokenDetailsExtractor {
    TokenDetails extract(String token);
}
