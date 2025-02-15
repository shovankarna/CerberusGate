package com.cdac.SpringCloudGateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import com.cdac.SpringCloudGateway.services.NonceService;

import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;

@Component
public class GatewayNonceFilter {

    @Autowired
    NonceService nonceService;

    public boolean validateNonce(ServerWebExchange exchange) {
        try {
            Boolean nonceEnabled = exchange.getAttribute("nonceEnabled");

            if (Boolean.TRUE.equals(nonceEnabled)) {
                String token = exchange.getRequest().getHeaders().getFirst("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    TokenDetails td = TokenExtract.getTokenDetails(token.substring(7));
                    String userId = td.getUserId();
                    String nonce = exchange.getRequest().getHeaders().getFirst("User-App-Token");

                    return nonce != null && nonceService.validateNonce(userId, nonce);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // Default to true if nonce validation is not enabled or an error occurs
        return true;
    }
}
