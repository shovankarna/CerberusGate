package com.cdac.SpringCloudGateway.jwt;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lazily verifies JWT using RS256 with public key from JWKS endpoint.
 */
@Component
public class JwtVerifier {

    @Value("${jwt.jwks-uri}")
    private String jwksUri;

    private final AtomicReference<RSAPublicKey> publicKeyRef = new AtomicReference<>();

    public Mono<Boolean> verify(String token) {
        try {
            RSAPublicKey key = getPublicKey(); // Lazy fetch + cache
            if (key == null)
                return Mono.just(false);

            JWSObject jws = JWSObject.parse(token);
            RSASSAVerifier verifier = new RSASSAVerifier(key);
            return Mono.just(jws.verify(verifier));
        } catch (Exception e) {
            e.printStackTrace(); // Optional logging
            return Mono.just(false);
        }
    }

    private RSAPublicKey getPublicKey() {
        RSAPublicKey existing = publicKeyRef.get();
        if (existing != null)
            return existing;

        synchronized (this) {
            if (publicKeyRef.get() != null)
                return publicKeyRef.get();
            try {
                JWKSet jwkSet = JWKSet.load(new URL(jwksUri));
                JWK jwk = jwkSet.getKeys().stream()
                        .filter(j -> j instanceof RSAKey)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No RSA key found in JWKS"));

                RSAPublicKey rsaKey = ((RSAKey) jwk).toRSAPublicKey();
                publicKeyRef.set(rsaKey);
                return rsaKey;
            } catch (Exception e) {
                e.printStackTrace(); // Optional
                return null;
            }
        }
    }
}
