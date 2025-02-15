package com.cdac.SpringCloudGateway.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Component
public class UserInfoTokenVerifier {

    private final WebClient webClient;

    @Value("${keycloak.base-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realmName;

    public UserInfoTokenVerifier() {
        // Initialize the WebClient
        this.webClient = WebClient.builder().build();
    }

    public Mono<Boolean> verifyToken(String token) {
        System.out.println("Starting JWT verification for token: " + token);

        return webClient.get()
                .uri(keycloakUrl + "/realms/" + realmName + "/protocol/openid-connect/userinfo")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(userInfo -> {
                    if (userInfo.containsKey("sub")) {
                        System.out.println("Token verification successful. Received user info: " + userInfo);
                        return Mono.just(true);
                    } else {
                        System.out.println("Token verification failed. 'sub' not found in user info: " + userInfo);
                        return Mono.just(false);
                    }
                })
                .onErrorReturn(false);
    }
}
