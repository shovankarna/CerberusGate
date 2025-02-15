// package com.cdac.SpringCloudGateway.config;

// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// import org.springframework.cloud.gateway.route.RouteLocator;
// import org.springframework.web.server.ServerWebExchange;
// import org.springframework.web.server.WebFilter;
// import org.springframework.web.server.WebFilterChain;

// import reactor.core.publisher.Mono;


// //USE OF SecurityMetadataFilter:
// //The SecurityMetadataFilter is a custom WebFilter that is being used to add metadata to the
// //ServerWebExchange object in Spring Cloud Gateway. This filter essentially extracts metadata from the
// //route configuration and attaches it to the exchange to make it available for downstream filters and handlers.


// public class SecurityMetadataFilter implements WebFilter {
//     private final RouteLocator routeLocator;

//     public SecurityMetadataFilter(RouteLocator routeLocator) {
//         this.routeLocator = routeLocator;
//     }

//     @Override
//     public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

//         try {

//             exchange.getAttributes().remove("isSecured");
//             exchange.getAttributes().remove("roles");
//             exchange.getAttributes().remove("rateLimit");
//             exchange.getAttributes().remove("nonceEnabled");

//             return routeLocator.getRoutes()
//                     .filterWhen(route -> route.getPredicate().apply(exchange))
//                     .next()
//                     .flatMap(route -> {
//                         Object securityConfig = route.getMetadata().get("securityConfig");
//                         //System.out
//                                 //.println("SecurityMetadataFilter - Requested Path: " + exchange.getRequest().getPath());
//                         //System.out.println("SecurityMetadataFilter - Security Config: " + securityConfig);

//                         if (securityConfig instanceof Map) {
//                             Map<String, Object> config = (Map<String, Object>) securityConfig;
//                             boolean isSecured = (boolean) config.getOrDefault("isSecured", true);
//                             exchange.getAttributes().put("isSecured", isSecured);
//                             // System.out.println("SecurityMetadataFilter - Is Secured: " + isSecured);

//                             //// ADD ROLES TO THE SERVER WEB EXCHANGE
//                             if (config.containsKey("roles")) {
//                                 Object rolesObj = config.get("roles");
//                                 if (rolesObj instanceof List) {
//                                     exchange.getAttributes().put("roles", rolesObj);
//                                 } else if (rolesObj instanceof Map) {
//                                     List<String> rolesList = ((Map<String, String>) rolesObj).values().stream()
//                                             .collect(Collectors.toList());
//                                     exchange.getAttributes().put("roles", rolesList);
//                                 }
//                                 // System.out.println(
//                                 // "SecurityMetadataFilter - Roles: " + exchange.getAttributes().get("roles"));
//                             }
//                             //System.out.println(
//                                    // "config.containsKey(\"rateLimit\") ------------" + config.containsKey("rateLimit"));

//                             //// ADD RATE-LIMITING TO THE SERVER WEB EXCHANGE
//                             if (config.containsKey("rateLimit")) {
//                                 Map<String, Object> rateLimitConfig = (Map<String, Object>) config.get("rateLimit");
//                                 exchange.getAttributes().put("rateLimit", rateLimitConfig);
//                                 // System.out.println("SecurityMetadataFilter - Rate Limit Config: " +
//                                 // rateLimitConfig);
//                             }

//                             //System.out.println(
//                                     //"config.containsKey(\"NONCE\") ------------" + config.containsKey("nonceEnabled"));
//                             //// ADD NONCE TO THE SERVER WEB EXCHANGE
//                             if (config.containsKey("nonceEnabled")) {
//                                 boolean nonceEnabled = (boolean) config.get("nonceEnabled");
//                                 exchange.getAttributes().put("nonceEnabled", nonceEnabled);
//                             }

//                         }
//                         // System.out.println("exchange --------------->>>>>>>>>>>>" +
//                         // exchange.getAttribute("rateLimit"));
//                         //System.out.println("exchange in metadata --------------->>>>>>>>>>>>" + exchange);
//                         return chain.filter(exchange);
//                     })
//                     .switchIfEmpty(chain.filter(exchange));

//         } catch (Exception e) {
//             // TODO: handle exception
//             //System.out.println("Error in SecurityMetadataFilter: " + e.getMessage());
//             e.printStackTrace();
//             return exchange.getResponse().setComplete();
//         }

//     }
// }
