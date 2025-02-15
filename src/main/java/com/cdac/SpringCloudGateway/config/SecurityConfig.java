package com.cdac.SpringCloudGateway.config;

import java.util.List;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    UserInfoTokenVerifier tokenVerifier;

    @Autowired
    GatewayRateLimitFilter rateLimitFilter;

    @Autowired
    GatewayNonceFilter nonceFilter;

    // @Autowired
    // RouteCache routeCache;

    @Autowired
    RouteLocator routeLocator;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Create the unified security filter
        UnifiedSecurityFilter unifiedSecurityFilter = new UnifiedSecurityFilter(
                routeLocator, rateLimitFilter, nonceFilter);

        return http
                .csrf(csrf -> csrf.disable())
                .addFilterAt(unifiedSecurityFilter, SecurityWebFiltersOrder.FIRST)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/public/**").permitAll() // Open paths do not require auth
                        .pathMatchers(HttpMethod.OPTIONS).permitAll() // Permit OPTIONS (CORS preflight)
                        .anyExchange().access(this::customAuthorizationLogic)) // Use custom authorization logic
                .build();
    }

    private Mono<AuthorizationDecision> customAuthorizationLogic(Mono<Authentication> authentication,
            AuthorizationContext context) {
        ServerWebExchange exchange = context.getExchange();

        // Logging all metadata before making authorization decisions
        System.out.println("URL: ===> " + exchange.getRequest().getPath());

        Boolean isSecured = exchange.getAttribute("isSecured");
        System.out.println("CustomAuthorizationLogic - Is Secured: " + isSecured);

        Object rolesObj = exchange.getAttribute("roles");
        System.out.println("CustomAuthorizationLogic - Required Roles: " + rolesObj);

        // If not secured, allow all access
        if (isSecured == null || !isSecured) {
            System.out.println("Route is not secured, allowing access");
            return Mono.just(new AuthorizationDecision(true));
        }

        if (!(rolesObj instanceof List)) {
            System.out.println("Roles are not in the expected format. Denying access.");
            return Mono.just(new AuthorizationDecision(false));
        }

        // Extract JWT token from Authorization header
        String token = extractTokenFromRequest(exchange);
        if (token == null) {
            System.out.println("JWT token not found, denying access.");
            return Mono.just(new AuthorizationDecision(false));
        }

        return tokenVerifier.verifyToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        System.out.println("JWT verification failed, denying access.");
                        return Mono.just(new AuthorizationDecision(false));
                    }

                    // JWT is valid, proceed with role verification
                    if (!(rolesObj instanceof List)) {
                        System.out.println("Roles are not in the expected format. Denying access.");
                        return Mono.just(new AuthorizationDecision(false));
                    }

                    List<String> requiredRoles = (List<String>) rolesObj;

                    // Extract roles from the token
                    TokenDetails tokenDetails = TokenExtract.getTokenDetails(token);
                    List<String> userRoles = tokenDetails.getRoles();

                    System.out.println("User Roles: " + userRoles);

                    boolean hasRequiredRole = requiredRoles.stream()
                            .map(String::toUpperCase)
                            .anyMatch(role -> userRoles.stream()
                                    .map(String::toUpperCase)
                                    .anyMatch(userRole -> userRole.equals(role)));
                    System.out.println("hasRequiredRole: ===> " + hasRequiredRole);
                    return Mono.just(new AuthorizationDecision(hasRequiredRole));
                });
    }

    private String extractTokenFromRequest(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
