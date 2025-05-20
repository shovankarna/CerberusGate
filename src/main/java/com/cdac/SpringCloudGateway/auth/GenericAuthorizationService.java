package com.cdac.SpringCloudGateway.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.cdac.SpringCloudGateway.jwt.JwtVerifier;
import com.cdac.SpringCloudGateway.jwt.TokenDetails;
import com.cdac.SpringCloudGateway.jwt.TokenDetailsExtractor;

import reactor.core.publisher.Mono;

/**
 * Generic authorization service that uses extractor and verifier for flexible
 * auth.
 */
@Service
@RequiredArgsConstructor
public class GenericAuthorizationService {

    private final JwtVerifier jwtVerifier;
    private final TokenDetailsExtractor extractor;

    public Mono<Boolean> verifyToken(String token) {
        return jwtVerifier.verify(token);
    }

    public TokenDetails extractDetails(String token) {
        return extractor.extract(token);
    }
}
