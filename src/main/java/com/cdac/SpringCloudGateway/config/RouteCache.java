package com.cdac.SpringCloudGateway.config;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RouteCache {

    private final RouteLocator routeLocator;
    private final Map<String, Route> routeCache = new ConcurrentHashMap<>();
    private final AtomicReference<Long> lastRefreshTime = new AtomicReference<>(System.currentTimeMillis());
    private static final long CACHE_REFRESH_INTERVAL = 60_000; // 1 minute

    public RouteCache(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeRouteCache() {
        refreshRouteCache();
        System.out.println("Initial route cache loaded with " + routeCache.size() + " routes.");
    }

    public Mono<Route> getMatchingRoute(ServerWebExchange exchange) {
        // Refresh the cache periodically based on the refresh interval
        if (System.currentTimeMillis() - lastRefreshTime.get() > CACHE_REFRESH_INTERVAL) {
            refreshRouteCache();
        }

        // Use the cached routes for matching
        return Flux.fromIterable(routeCache.values())
                .filterWhen(route -> route.getPredicate().apply(exchange)) // Match predicates reactively
                .next()
                .switchIfEmpty(Mono.fromRunnable(() -> System.out
                        .println("No matching route found for request: " + exchange.getRequest().getURI())));
    }

    private void refreshRouteCache() {
        routeLocator.getRoutes()
                .collectList()
                .doOnNext(routes -> {
                    routeCache.clear();
                    routes.forEach(route -> routeCache.put(route.getId(), route));
                    lastRefreshTime.set(System.currentTimeMillis());
                    System.out.println("Route cache refreshed with " + routeCache.size() + " routes.");
                })
                .subscribe(); // Subscribe to trigger route loading
    }
}
